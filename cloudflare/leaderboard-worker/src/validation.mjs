const MAX_DISPLAY_NAME_LENGTH = 20;
const MAX_SCORE = 999999999;
const MAX_WAVE = 999;
const MAX_DURATION_MS = 24 * 60 * 60 * 1000;

export function normalizeInitials(value) {
  return String(value ?? "")
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "")
    .slice(0, 3);
}

export function normalizeDisplayName(value) {
  return String(value ?? "")
    .trim()
    .replace(/\s+/g, " ")
    .replace(/[^\p{L}\p{N} ._'#-]/gu, "")
    .slice(0, MAX_DISPLAY_NAME_LENGTH);
}

export function clampLimit(value, fallback = 10) {
  const parsed = Number.parseInt(String(value ?? ""), 10);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.max(1, Math.min(50, parsed));
}

export function validatePlayerPayload(payload) {
  const displayName = normalizeDisplayName(payload?.display_name);
  if (!displayName) return { ok: false, error: "display_name_required" };
  return { ok: true, displayName };
}

export function validateScorePayload(payload) {
  const playerId = String(payload?.player_id ?? "").trim();
  const playerToken = String(payload?.player_token ?? "").trim();
  const runId = String(payload?.run_id ?? "").trim();
  const initials = normalizeInitials(payload?.initials);
  const score = Number(payload?.score);
  const wave = Number(payload?.wave);
  const durationMs =
    payload?.duration_ms === undefined || payload?.duration_ms === null
      ? null
      : Number(payload.duration_ms);
  const gameVersion = String(payload?.game_version ?? "").trim().slice(0, 40) || null;
  const host = String(payload?.host ?? "").trim();

  if (!looksLikeUuid(playerId)) return { ok: false, error: "player_id_invalid" };
  if (playerToken.length < 24) return { ok: false, error: "player_token_invalid" };
  if (!looksLikeUuid(runId)) return { ok: false, error: "run_id_invalid" };
  if (initials.length < 1) return { ok: false, error: "initials_required" };
  if (!Number.isInteger(score) || score <= 0 || score > MAX_SCORE) {
    return { ok: false, error: "score_invalid" };
  }
  if (!Number.isInteger(wave) || wave <= 0 || wave > MAX_WAVE) {
    return { ok: false, error: "wave_invalid" };
  }
  if (
    durationMs !== null &&
    (!Number.isInteger(durationMs) || durationMs < 0 || durationMs > MAX_DURATION_MS)
  ) {
    return { ok: false, error: "duration_invalid" };
  }
  if (!["browser", "desktop"].includes(host)) {
    return { ok: false, error: "host_invalid" };
  }

  return {
    ok: true,
    score: {
      playerId,
      playerToken,
      runId,
      initials,
      score,
      wave,
      durationMs,
      gameVersion,
      host
    }
  };
}

export function looksLikeUuid(value) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
    String(value ?? "")
  );
}
