local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local current = redis.call("GET", key)

if current and tonumber(current) >= limit then
    return 0
else
    current = redis.call("INCR", key)
    if current == 1 then
        redis.call("PEXPIRE", key, window)
    end
    return 1
end
