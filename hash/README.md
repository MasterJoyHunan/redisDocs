### 常用
* `hset key field value` 为散列一个 field 设置 value
* `hmset key field value [field value ...]` 为散列一个或多个 field 设置 value
* `hget key field` 获取一个散列 field 对应的 value
* `hmget key field [field ...]` 获取一个或多个散列 field 对应的 value
* `hgetall key` 获取一个散列所有的 field 和 value
* `hdel key field [field ...]` 删除一个散列一个或多个 field
* `hlen key` 获取一个散列所有键值对的个数
### 不常用
* `hexists key field` 获取一个散列所有键值对的个数
* `hkeys key` 获取一个散列所有键
* `hvals key` 获取一个散列所有值
* `hincrby key field number` 为一个散列的值自增 number
* `hincrbyfloat key field number` 为一个散列的值自增浮点数 number
