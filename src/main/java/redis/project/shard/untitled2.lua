local max = 2
local skey = ARGV[1] == 'lpush' and KEYS[2] or KEYS[3]  --xx:first / xx:last
local shard = redis.call('get', skey) or '0' -- xx:0/1/3/
while 1 do 
   local current = tonumber(redis.call('llen', KEYS[1]..shard)) -- 当前分片里面有多少个元素
   local topush = math.min(#ARGV - 1, max - current)  -- 可以加入多少个元素进当前分片
   if topush > 0 then  
       redis.call(ARGV[1], KEYS[1] .. shard, unpack(ARGV, 2, topush + 1)) 
       return topush 
   end 
   shard = redis.call(ARGV[1] == 'lpush' and 'decr' or 'incr', skey) 
end