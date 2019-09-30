### Redis中，每个key的说明
|key|类型|描述|存储内容|
|:-:|:-:|:-:|:-:|
|article:id|hash|文章列表-文章ID|文章内容|
|vote:id|set|用户投票表-文章ID|用户ID|
|article_score|set|文章投票排名|文章=>分值|
