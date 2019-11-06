local myGroup = redis.call("zrange", "SEEN:" .. KEYS[1], 0, -1, "withscores") -- 查所有我加入的群组
local msg = {}
local groupId = 0
for index, v in ipairs(myGroup) do --循环我加入的群组
    if index % 2 == 0 then
        -- groupId => 群组ID |  v => 已阅读的message
        -- 1. 返回所有未读消息
        local message = redis.call("zrangebyscore", "CHAT:GROUP_MESSAGE:" .. groupId, v + 1, "inf", "withscores")
        -- 2. 更新 SEEN:N 和 CHAT:GROUP:N 的阅读
        if not (next(message) == nil) then
            local max_read = 0
            local arr_index = 1
            msg[groupId] = {}
            for index2, vv in ipairs(message) do
                if index2 % 2 == 0 and tonumber(vv) > max_read then
                    max_read = tonumber(vv)
                else
                    msg[groupId][arr_index] = vv
                    arr_index = arr_index + 1
                end
            end
            redis.call("zadd", "SEEN:" .. KEYS[1], max_read, groupId)
            redis.call("zadd", "CHAT:GROUP:" .. groupId, max_read, KEYS[1])
            -- 3. 删除所有人已读的message
            local min_not_read = redis.call("zrange", "CHAT:GROUP:" .. groupId, 0, 0, "withscores")
            if not (next(min_not_read) == nil) then
                local max_score = tonumber(min_not_read[2])
                redis.call("zremrangebyscore", "CHAT:GROUP_MESSAGE:" .. groupId, 0, max_score)
            end
        end
    else
        groupId = v
    end
end
if next(msg) == nil then
    return nil
else
    return cjson.encode(msg)
end
