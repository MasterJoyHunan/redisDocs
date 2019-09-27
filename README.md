# Redis常用命令集合

### [string](https://github.com/MasterJoyHunan/redisDocs/tree/master/string)
> 字符串
### [list](https://github.com/MasterJoyHunan/redisDocs/tree/master/list)
> 列表
### [set](https://github.com/MasterJoyHunan/redisDocs/tree/master/set)
> 集合
### [zset](https://github.com/MasterJoyHunan/redisDocs/tree/master/zset)
> 有序集合
### [hash](https://github.com/MasterJoyHunan/redisDocs/tree/master/hash)
> 散列
### [publish/subscribe](https://github.com/MasterJoyHunan/redisDocs/tree/master/p_s)
> 发布、订阅
### 通用命令
* `del key` -- 删除
* `type key` -- 查看某个键属于什么结构
* `rename old_key new_key` -- 重命名键
* `sort key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern …]] [ASC | DESC] [ALPHA] [STORE destination]` -- 排序