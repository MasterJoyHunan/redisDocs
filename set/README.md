### 常用
* `sadd key value [value ...]` 将一个或多个元素加入集合
* `srem key value [value ...]` 将一个或多个元素从集合中删除
* `smembers key` 返回集合中所有元素
* `scard key` 返回集合中所有元素的数量
* `sismember key value` 检查 value 是否存在于 key 中
* `srandmember key [count]` 随机返回集合中 count 绝对值个元素，如果count < 0 则有可能返回重复元素
* `spop key` 随机删除集合中一个元素
* `smove source-key dest-key value` 将 source-key 中的元素 value 移动到 dest-key
### 不常用
* `sdiff key [key ...]` 求多个集合中的差集
* `sdiffstore dest-key key [key ...]` 求多个集合中的差集 将结果存在 dest-key 中
* `sinter key` 求多个集合中的交集
* `sinterstore dest-key key [key ...]` 求多个集合中的交集 将结果存在 dest-key 中
* `sunion key` 求多个集合中的并集
* `sunionstore dest-key key [key ...]` 求多个集合中的并集 将结果存在 dest-key 中

并集：以属于A或属于B的元素为元素的集合成为A与B的并（集） A | B
交集：以属于A且属于B的元素为元素的集合成为A与B的交（集） A & B
差：以属于A而不属于B的元素为元素的集合成为A与B的差（集） A ^ B