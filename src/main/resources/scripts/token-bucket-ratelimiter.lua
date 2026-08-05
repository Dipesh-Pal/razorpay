-- Token bucket rate limiter (Redis 7+/8+).
-- Invoked via EVAL/EVALSHA; no shebang because `#!lua name=` is a Redis Functions-only construct.
-- KEYS[1] : bucket key
-- ARGV[1] : capacity        (max tokens, > 0)
-- ARGV[2] : refillPerSec    (tokens added per second, > 0)
-- ARGV[3] : nowMs           (optional; caller-supplied time in ms; 0 or empty => use server TIME)
-- ARGV[4] : ttlSeconds      (idle bucket expiry, > 0)
-- ARGV[5] : cost            (optional; tokens to consume, default 1)
-- Returns : { isAllowed(0|1), remainingTokens(int), retryAfterMs(int) }
--           retryAfterMs = -1 when the request can never be served (cost > capacity)

local key          = KEYS[1]
local capacity     = tonumber(ARGV[1])
local refillPerSec = tonumber(ARGV[2])
local nowMsArg     = tonumber(ARGV[3])
local ttlSeconds   = tonumber(ARGV[4])
local cost         = tonumber(ARGV[5]) or 1

if not capacity or not refillPerSec or not ttlSeconds
        or capacity <= 0 or refillPerSec <= 0 or ttlSeconds <= 0 or cost <= 0 then
    return redis.error_reply('INVALID_ARGS')
end

local nowMs
if nowMsArg and nowMsArg > 0 then
    nowMs = nowMsArg
else
    local t = redis.call('TIME')
    nowMs = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
end
if cost > capacity then
    return { 0, 0, -1 }
end

local data         = redis.call('HMGET', key, 'tokens', 'ts')
local tokens       = tonumber(data[1]) or capacity
local lastRefillTs = tonumber(data[2]) or nowMs

local elapsedSec = math.max(0, (nowMs - lastRefillTs) / 1000)
if elapsedSec > 0 then
    tokens = math.min(capacity, tokens + elapsedSec * refillPerSec)
end

local allowed = 0
if tokens >= cost then
    tokens  = tokens - cost
    allowed = 1
end

redis.call('HSET', key, 'tokens', tokens, 'ts', nowMs)
redis.call('EXPIRE', key, ttlSeconds)

local retryAfterMs = 0
if allowed == 0 then
    retryAfterMs = math.max(0, math.ceil(((cost - tokens) / refillPerSec) * 1000))
end

return { allowed, math.floor(tokens), retryAfterMs }
