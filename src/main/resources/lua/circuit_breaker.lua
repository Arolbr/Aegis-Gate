-- KEYS[1] key
-- ARGV[1] failureThreshold
-- ARGV[2] minimumRequest
-- ARGV[3] window(ms)
-- ARGV[4] resetTimeout(ms)
-- ARGV[5] failure(0或1)

local key = KEYS[1]
local threshold = tonumber(ARGV[1])
local minReq = tonumber(ARGV[2])
local window = tonumber(ARGV[3])
local reset = tonumber(ARGV[4])
local fail = tonumber(ARGV[5])

local stateKey = key .. ":state"
local countKey = key .. ":count"
local failKey = key .. ":fail"

-- 检查熔断状态
local state = redis.call("GET", stateKey)
if state == "open" then
    return 0
end

-- 更新请求数和失败数
local count = redis.call("INCR", countKey)
if count == 1 then
    redis.call("PEXPIRE", countKey, window)
    redis.call("SET", failKey, 0, "PX", window)
end

if fail == 1 then
    redis.call("INCR", failKey)
end

-- 判断是否触发熔断
local total = tonumber(redis.call("GET", countKey))
local failures = tonumber(redis.call("GET", failKey))
if total >= minReq and failures / total >= threshold then
    redis.call("SET", stateKey, "open", "PX", reset)
    return 0
end

return 1
