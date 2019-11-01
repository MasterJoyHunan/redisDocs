#### redis 结构备注

|key|结构|说明|
|:-:|:-:|:-:|:-:|
| AD:LOCATION:(location)| set | 省市区对应的广告ID|
| AD:INDEX:(word)| set | 关键词里含有的广告id|
| AD:WORD:(ad_id)| set | 广告id含有的关键词|
| AD:TYPES:| hash | 广告id => 广告计费方式|
| AD:ECPM:| zset | 广告id => 千次显示预估收益|
| AD:BASE_COST:| zset | 广告id => 单次（显示、点击、购买）操作收益|
| AD:SERVED:| string | ??? |
| AD:MATCHED:(???)| zset | ??? |
| AD:VIEWS:(ad_id)| zset | 广告关键词 => 查看的次数 自己=>被查看的次数 |
| AD:TYPE_VIEWS:(type)| string | 广告计费方式 被匹配的次数 |
| AD:CLICKS:(ad_id)| zset | 关键词(点击) => 被匹配的次数 |
| AD:ACTIONS:(ad_id)| zset | 关键词(操作) => 被匹配的次数 |
| AD:(type):ACTION| string | 广告(计费方式)操作量 |
| AD:(type):CLICKS| string | 广告计费方式点击量 |
| AD:WORD_EXT:| zset | 广告关键词 => 附加值|


