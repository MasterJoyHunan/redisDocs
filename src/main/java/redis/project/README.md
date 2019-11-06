### Redis中，每个key的说明
#### 文章缓存
|key|类型|描述|存储内容|
|:-:|:-:|:-:|:-:|
|article:article:<id>|hash|文章列表-文章ID|文章内容|
|article:vote:<id>|set|用户投票表-文章ID|用户ID|
|article:article_score|zset|文章投票排名|文章=>分值|
|article:group:<name>|set|文章分类|文章分类对应的文件ID|
|article:score_group:<name>|zset|文章分类|文章分类对应的文件ID=>分值|

#### 用户缓存
|key|类型|描述|存储内容|
|:-:|:-:|:-:|:-:|
|user:login|hash|token|token=>的用户ID|
|user:last|zset|token|token=>最后使用的时间|
|user:view:<uid>|zset|观看文章列表|保留最近观看25条文章记录|
