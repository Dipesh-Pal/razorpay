-- Atomically blocklist an access + refresh JTI pair.
-- KEYS[1] = access blocklist key   (may be "" to skip)
-- KEYS[2] = refresh blocklist key  (may be "" to skip)
-- ARGV[1] = access TTL in seconds  (integer; <= 0 means skip)
-- ARGV[2] = refresh TTL in seconds (integer; <= 0 means skip)
--
-- Semantics: SET NX EX for each side so replay-blocklist is a no-op
-- (preserves the original TTL of a key that was already blocklisted).
-- Returns the number of keys actually written (0, 1, or 2).

local written = 0

local accessTtl  = tonumber(ARGV[1])
local refreshTtl = tonumber(ARGV[2])

if KEYS[1] ~= '' and accessTtl ~= nil and accessTtl > 0 then
    if redis.call('SET', KEYS[1], '1', 'EX', accessTtl, 'NX') then
        written = written + 1
    end
end

if KEYS[2] ~= '' and refreshTtl ~= nil and refreshTtl > 0 then
    if redis.call('SET', KEYS[2], '1', 'EX', refreshTtl, 'NX') then
        written = written + 1
    end
end

return written
