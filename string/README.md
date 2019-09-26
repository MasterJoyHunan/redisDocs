* `set key` 增、改
* `get key`  查
* `del key`  删
* `incr key`  自增 + 1
* `incrby key number` 自增 + number
* `incrbyfloat key number`  浮点数自增 + number
* `decr ` 自减 - 1
* `decrby key number` 自减 - number
* `append key value` 追加到key的后面
* `setrange key start end` 为字符串key从start到end的位置覆盖新值
* `getrange key start end` 获取字符串key从start到end的位置的值
* `setbit key offset value` 位图操作 设置key的offset位置的二进制为value(0,1)
* `getbit key offset` 位图操作 获取key的offset位置的二进制
* `bitcount key [start end]` 位图操作 获取key里为1的数量，设置start和end则对置顶范围内的二进制进行统计
* `bitop operation dest-key key1 key2 [key...]` 对N个位图进行操作 AND、OR、XOR、NOT。结果存储在dest-key中
