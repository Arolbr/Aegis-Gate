-- KEYS[1] = methodKey
-- ARGV[1] = failureThreshold
-- ARGV[2] = window (毫秒)
-- ARGV[3] = openTime (毫秒)
-- ARGV[4] = isFailure (1=失败, 0=成功)

local key = KEYS[1]
local threshold = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local openTime = tonumber(ARGV[3])
local isFailure = tonumber(ARGV[4])

local statusKey = key .. ":status"
local failKey = key .. ":fail"
local totalKey = key .. ":total"

-- 检查熔断状态
local status = redis.call("GET", statusKey)
if status == "OPEN" then
    return 0
elseif status == "HALF_OPEN" then
    -- 允许一次尝试，交给应用更新状态
else
    -- 更新计数
    redis.call("INCR", totalKey)
    if isFailure == 1 then
        redis.call("INCR", failKey)
    end
    redis.call("PEXPIRE", failKey, window)
    redis.call("PEXPIRE", totalKey, window)

    local failCount = tonumber(redis.call("GET", failKey) or "0")
    local totalCount = tonumber(redis.call("GET", totalKey) or "0")
    if totalCount > 0 and failCount / totalCount >= threshold then
        redis.call("SET", statusKey, "OPEN")
        redis.call("PEXPIRE", statusKey, openTime)
        return 0
    end
end

return 1
