### 常用
* `zadd key number item [number item ...]` 将一个或多个含有分值的成员添加到有序集合
* `zrem key item [item ...]` 将一个或多个成员从有序集合中删除
* `zcount key start end` 返回有序集合中分值 start 到 end 之间的成员数量
* `zscore key item` 返回有序集合中 item 的分值
* `zcard key` 返回有序集合中元素个数

### 获取排名
* `zrank key item` 返回有序集合中 item 的排名（从小到大排序）
* `zrevrank key item` 返回有序集合中 item 的排名（从大到小排序）

### 获取区间（根据分值）
* `zrange key start end [withscores]` 返回有序集合中分值“排名” start 到 end 之间的成员（从小到大排序）
* `zrevrange key start end [withscores]` 返回有序集合中分值“排名” start 到 end 之间的成员（从大到小排序）
* `zrangebyscore key start end [withscores] [limit offset count]` 返回有序集合中分值 start 到 end 之间的成员（从小到大排序）
* `zrevrangebyscore key start end [withscores] [limit offset count]` 返回有序集合中分值 start 到 end 之间的成员（从大到小排序）

### 删除
* `zremrangebyrank key start end` 将有序集合中分值排名 start 到 end 之间的成员移除（从小到大排序）
* `zremrangebyscore key start end` 将有序集合中分值 start 到 end 之间的成员移除（从小到大排序）

### 集合操作
* `zinterstore dest-key key-count key [key ...]` 交集
* `zunionstore dest-key key-count key [key ...]` 并集

并集：以属于A或属于B的元素为元素的集合成为A与B的并（集） A | B
交集：以属于A且属于B的元素为元素的集合成为A与B的交（集） A & B
差：以属于A而不属于B的元素为元素的集合成为A与B的差（集） A ^ B
