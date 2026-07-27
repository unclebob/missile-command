import {
  clampLimit,
  validatePlayerPayload,
  validateScorePayload
} from "./validation.mjs";

const JSON_HEADERS = { "content-type": "application/json; charset=utf-8" };
const SCORE_MINUTE_LIMIT_MS = 60 * 1000;
const SCORE_DAY_LIMIT_MS = 24 * 60 * 60 * 1000;
const SCORE_DAY_LIMIT_COUNT = 20;

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return corsResponse(env);

    const url = new URL(request.url);
    try {
      if (request.method === "GET" && url.pathname === "/health") {
        return json(env, { ok: true });
      }
      if (request.method === "GET" && url.pathname === "/leaderboard-info") {
        return json(env, leaderboardInfo(env));
      }
      if (request.method === "GET" && url.pathname === "/leaderboard") {
        return getLeaderboard(request, env);
      }
      if (request.method === "POST" && url.pathname === "/players") {
        return createPlayer(request, env);
      }
      if (request.method === "POST" && url.pathname === "/scores") {
        return submitScore(request, env);
      }
      return json(env, { error: "not_found" }, 404);
    } catch (error) {
      console.error(error);
      return json(env, { error: "server_error" }, 500);
    }
  }
};

export function leaderboardInfo(env) {
  return {
    leaderboard_id: env.LEADERBOARD_ID || "custom-leaderboard",
    display_name: env.LEADERBOARD_DISPLAY_NAME || "Custom leaderboard",
    operator: env.LEADERBOARD_OPERATOR || "",
    policy: env.LEADERBOARD_POLICY || "best-score-per-player"
  };
}

async function getLeaderboard(request, env) {
  const url = new URL(request.url);
  const limit = clampLimit(url.searchParams.get("limit"), 10);
  const { results } = await env.DB.prepare(
    `
    select
      row_number() over (order by ranked.score desc, ranked.created_at asc) as rank,
      ranked.public_code,
      ranked.display_name,
      ranked.initials,
      ranked.score,
      ranked.wave,
      ranked.created_at
    from (
      select
        p.public_code,
        p.display_name,
        s.initials,
        s.score,
        s.wave,
        s.created_at,
        row_number() over (
          partition by s.player_id
          order by s.score desc, s.created_at asc
        ) as player_rank
      from scores s
      join players p on p.id = s.player_id
    ) ranked
    where ranked.player_rank = 1
    order by ranked.score desc, ranked.created_at asc
    limit ?
    `
  )
    .bind(limit)
    .all();

  return json(env, {
    leaderboard: leaderboardInfo(env),
    scores: results ?? []
  });
}

async function createPlayer(request, env) {
  const payload = await readJson(request);
  const valid = validatePlayerPayload(payload);
  if (!valid.ok) return json(env, { error: valid.error }, 400);

  const id = crypto.randomUUID();
  const token = randomToken();
  const publicCode = await uniquePublicCode(env);
  const now = new Date().toISOString();
  const tokenHash = await sha256(token);

  await env.DB.prepare(
    `
    insert into players (id, public_code, token_hash, display_name, created_at, updated_at)
    values (?, ?, ?, ?, ?, ?)
    `
  )
    .bind(id, publicCode, tokenHash, valid.displayName, now, now)
    .run();

  return json(env, {
    player_id: id,
    player_token: token,
    public_code: publicCode,
    display_name: valid.displayName
  });
}

async function submitScore(request, env) {
  const payload = await readJson(request);
  const valid = validateScorePayload(payload);
  if (!valid.ok) return json(env, { accepted: false, error: valid.error }, 400);

  const score = valid.score;
  const player = await env.DB.prepare(
    "select id, token_hash from players where id = ?"
  )
    .bind(score.playerId)
    .first();
  if (!player) return json(env, { accepted: false, error: "player_not_found" }, 404);

  const tokenHash = await sha256(score.playerToken);
  if (!constantTimeEqual(tokenHash, player.token_hash)) {
    return json(env, { accepted: false, error: "player_token_invalid" }, 403);
  }

  const nowDate = new Date();
  const now = nowDate.toISOString();
  const ip = clientIp(request);
  const ipHash = ip ? await sha256(ip) : null;
  const playerLimit = rateLimitResult(
    await recentPlayerScoreTimes(env, score.playerId, nowDate),
    nowDate.getTime()
  );
  if (playerLimit) {
    return rateLimitResponse(env, "player_id", playerLimit);
  }
  if (ipHash) {
    const ipLimit = rateLimitResult(
      await recentIpScoreTimes(env, ipHash, nowDate),
      nowDate.getTime()
    );
    if (ipLimit) {
      return rateLimitResponse(env, "ip", ipLimit);
    }
  }

  const result = await env.DB.prepare(
    `
    insert or ignore into scores (
      id, run_id, player_id, initials, score, wave, duration_ms,
      game_version, host, created_at, ip_hash
    )
    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `
  )
    .bind(
      crypto.randomUUID(),
      score.runId,
      score.playerId,
      score.initials,
      score.score,
      score.wave,
      score.durationMs,
      score.gameVersion,
      score.host,
      now,
      ipHash
    )
    .run();

  return json(env, {
    accepted: true,
    duplicate: result.meta?.changes === 0
  });
}

async function recentPlayerScoreTimes(env, playerId, nowDate) {
  const since = new Date(nowDate.getTime() - SCORE_DAY_LIMIT_MS).toISOString();
  const { results } = await env.DB.prepare(
    `
    select created_at
    from scores
    where player_id = ? and created_at >= ?
    order by created_at desc
    `
  )
    .bind(playerId, since)
    .all();
  return (results ?? []).map((row) => row.created_at);
}

async function recentIpScoreTimes(env, ipHash, nowDate) {
  const since = new Date(nowDate.getTime() - SCORE_DAY_LIMIT_MS).toISOString();
  const { results } = await env.DB.prepare(
    `
    select created_at
    from scores
    where ip_hash = ? and created_at >= ?
    order by created_at desc
    `
  )
    .bind(ipHash, since)
    .all();
  return (results ?? []).map((row) => row.created_at);
}

export function rateLimitResult(createdAts, nowMs) {
  const times = (createdAts ?? [])
    .map((createdAt) => Date.parse(createdAt))
    .filter((time) => Number.isFinite(time) && time <= nowMs);
  const inMinute = times.filter((time) => nowMs - time < SCORE_MINUTE_LIMIT_MS);
  if (inMinute.length >= 1) {
    const newest = Math.max(...inMinute);
    return {
      window: "minute",
      retryAfterSeconds: Math.max(1, Math.ceil((SCORE_MINUTE_LIMIT_MS - (nowMs - newest)) / 1000))
    };
  }
  const inDay = times.filter((time) => nowMs - time < SCORE_DAY_LIMIT_MS);
  if (inDay.length >= SCORE_DAY_LIMIT_COUNT) {
    const oldest = Math.min(...inDay);
    return {
      window: "day",
      retryAfterSeconds: Math.max(1, Math.ceil((SCORE_DAY_LIMIT_MS - (nowMs - oldest)) / 1000))
    };
  }
  return null;
}

export function clientIpFromHeaders(headers) {
  const cfIp = headers.get("cf-connecting-ip");
  if (cfIp) return cfIp.trim();
  const forwarded = headers.get("x-forwarded-for");
  if (!forwarded) return null;
  return forwarded.split(",")[0]?.trim() || null;
}

function clientIp(request) {
  return clientIpFromHeaders(request.headers);
}

function rateLimitResponse(env, subject, limit) {
  return json(
    env,
    {
      accepted: false,
      error: "rate_limited",
      subject,
      window: limit.window,
      retry_after_seconds: limit.retryAfterSeconds
    },
    429,
    { "retry-after": String(limit.retryAfterSeconds) }
  );
}

async function uniquePublicCode(env) {
  for (let i = 0; i < 8; i += 1) {
    const code = randomPublicCode();
    const existing = await env.DB.prepare("select id from players where public_code = ?")
      .bind(code)
      .first();
    if (!existing) return code;
  }
  throw new Error("public_code_exhausted");
}

function randomPublicCode() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = new Uint8Array(6);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => alphabet[b % alphabet.length]).join("");
}

function randomToken() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return base64Url(bytes);
}

function base64Url(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

async function sha256(value) {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return base64Url(new Uint8Array(digest));
}

function constantTimeEqual(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i += 1) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}

async function readJson(request) {
  try {
    return await request.json();
  } catch (_error) {
    return {};
  }
}

function json(env, payload, status = 200, headers = {}) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { ...JSON_HEADERS, ...corsHeaders(env), ...headers }
  });
}

function corsResponse(env) {
  return new Response(null, { status: 204, headers: corsHeaders(env) });
}

function corsHeaders(env) {
  return {
    "access-control-allow-origin": env.ALLOWED_ORIGIN || "*",
    "access-control-allow-methods": "GET,POST,OPTIONS",
    "access-control-allow-headers": "content-type",
    "access-control-max-age": "86400"
  };
}
