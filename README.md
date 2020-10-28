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
```
    sort 
        key -- 对哪个集合进行排序
        [BY pattern] -- 对其他键的元素作为权重，为 key 进行排序 * 表示占位符 *->field 表示在某个hash下面的field
        [LIMIT offset count] -- 对返回进行限制
        [GET pattern [GET pattern …]] -- 获取其他键的元素作为返回结果
        [ASC | DESC] -- 升序，倒序
        [ALPHA] -- 按字符串进行排序
        [STORE destination] -- 将结果保存
        
```

### 鸣谢

特别感谢 [JetBrains](https://www.jetbrains.com/?from=redisDocs) 为本开源项目提供免费的 [IntelliJ GoLand](https://www.jetbrains.com/go/?from=redisDocs) 授权

<p>
 <a href="https://www.jetbrains.com/?from=redisDocs">
   <img height="200" src="https://www.fdevops.com/wp-content/uploads/2020/09/1599213857-jetbrains-variant-4.png">
 </a>
</p>
