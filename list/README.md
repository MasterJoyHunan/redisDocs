### 常用
* `rpush key value [value ...]` 将一个或多个值插入的列表右端
* `lpush key value [value ...]` 将一个或多个值插入的列表左端
* `rpop key` 弹出右端元素
* `lpop key` 弹出左端元素
* `lrange key start end` 从列表返回 start 到 end 范围内的元素
* `lindex key index` 获取列表索引为index的值
* `ltrim key start end` 对列表进行裁剪
### 不常用
* `blpop key [key ...] timeout` 从第一个非空列表中弹出最左端元素，或在 timeout 秒内阻塞并等待可弹出的元素
* `brpop key [key ...] timeout` 从第一个非空列表中弹出最右端元素，或在 timeout 秒内阻塞并等待可弹出的元素
* `rpoplpush source-key dest-key` 从 source-key 右端弹出元素推入 dest-key
* `brpoplpush source-key dest-key timeout` 从 source-key 右端弹出元素推入 dest-key。如果 source-key 为空，则等待 timeout 秒内阻塞并等待 source-key 可弹出的元素
* `lrem key count value` 从 key 中删除值为 value 的元素 count = 0 全部删除、 count > 0 从左往右删除 count 个 、 count < 0 从右往左删除 count 的绝对值个 