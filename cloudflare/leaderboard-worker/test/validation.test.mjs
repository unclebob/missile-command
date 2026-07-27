import test from "node:test";
import assert from "node:assert/strict";
import {
  clientIpFromHeaders,
  rateLimitResult
} from "../src/index.mjs";
import {
  clampLimit,
  normalizeDisplayName,
  normalizeInitials,
  validatePlayerPayload,
  validateScorePayload
} from "../src/validation.mjs";

test("normalizes initials using the game display rules", () => {
  assert.equal(normalizeInitials("bob"), "BOB");
  assert.equal(normalizeInitials("a!2?c9"), "A2C");
});

test("normalizes display names conservatively", () => {
  assert.equal(normalizeDisplayName("  Uncle   Bob!!!  "), "Uncle Bob");
  assert.equal(normalizeDisplayName("abcdefghijklmnopqrstuvwxyz"), "abcdefghijklmnopqrst");
});

test("validates player creation payload", () => {
  assert.deepEqual(validatePlayerPayload({ display_name: "ACE" }), {
    ok: true,
    displayName: "ACE"
  });
  assert.deepEqual(validatePlayerPayload({ display_name: "   " }), {
    ok: false,
    error: "display_name_required"
  });
});

test("validates score submission payload", () => {
  const payload = {
    player_id: "550e8400-e29b-41d4-a716-446655440000",
    player_token: "abcdefghijklmnopqrstuvwxyz",
    run_id: "650e8400-e29b-41d4-a716-446655440000",
    initials: "ace",
    score: 12345,
    wave: 9,
    duration_ms: 123000,
    game_version: "1.0.0",
    host: "browser"
  };
  const result = validateScorePayload(payload);
  assert.equal(result.ok, true);
  assert.equal(result.score.initials, "ACE");
  assert.equal(result.score.host, "browser");
});

test("rejects invalid score submissions", () => {
  assert.equal(validateScorePayload({}).error, "player_id_invalid");
  assert.equal(
    validateScorePayload({
      player_id: "550e8400-e29b-41d4-a716-446655440000",
      player_token: "abcdefghijklmnopqrstuvwxyz",
      run_id: "650e8400-e29b-41d4-a716-446655440000",
      initials: "ACE",
      score: -1,
      wave: 1,
      host: "browser"
    }).error,
    "score_invalid"
  );
});

test("clamps leaderboard limits", () => {
  assert.equal(clampLimit("20"), 20);
  assert.equal(clampLimit("0"), 1);
  assert.equal(clampLimit("500"), 50);
  assert.equal(clampLimit("nope"), 10);
});

test("rate limits accepted scores by minute and day windows", () => {
  const now = Date.parse("2026-07-27T12:00:00.000Z");
  assert.deepEqual(rateLimitResult(["2026-07-27T11:59:30.000Z"], now), {
    window: "minute",
    retryAfterSeconds: 30
  });

  const twentyInDay = Array.from({ length: 20 }, (_, i) =>
    new Date(now - (i + 2) * 60 * 60 * 1000).toISOString()
  );
  assert.equal(rateLimitResult(twentyInDay, now).window, "day");
  assert.equal(rateLimitResult(["2026-07-26T11:59:59.000Z"], now), null);
});

test("extracts client IP from Cloudflare or forwarded headers", () => {
  assert.equal(
    clientIpFromHeaders(new Headers({ "cf-connecting-ip": "203.0.113.10" })),
    "203.0.113.10"
  );
  assert.equal(
    clientIpFromHeaders(new Headers({ "x-forwarded-for": "198.51.100.4, 10.0.0.1" })),
    "198.51.100.4"
  );
  assert.equal(clientIpFromHeaders(new Headers()), null);
});
