#!/usr/bin/env bash
# https://github.com/medcl/elasticsearch-analysis-ik

ES_HTTP_PORT=9200
TEST_IK_INDEX_NAME=test-ik

# 1. create a index

curl -XPUT http://localhost:${ES_HTTP_PORT}/${TEST_IK_INDEX_NAME}

# 2. create a mapping

curl -XPUT http://localhost:${ES_HTTP_PORT}/${TEST_IK_INDEX_NAME}/_mapping/_doc -H 'Content-Type:application/json' -d'
{
        "properties": {
            "content": {
                "type": "text",
                "analyzer": "ik_max_word",
                "search_analyzer": "ik_smart"
            }
        }

}
'

# 3. index some docs

curl -XPUT http://localhost:${ES_HTTP_PORT}/${TEST_IK_INDEX_NAME}/_doc/1 -H 'Content-Type:application/json' -d'
{"content":"美国留给伊拉克的是个烂摊子吗"}
'

curl -PUT http://localhost:${ES_HTTP_PORT}/${TEST_IK_INDEX_NAME}/_doc/2 -H 'Content-Type:application/json' -d'
{"content":"公安部：各地校车将享最高路权"}
'

curl -XPUT http://localhost:${ES_HTTP_PORT}/${TEST_IK_INDEX_NAME}/_doc/3 -H 'Content-Type:application/json' -d'
{"content":"中韩渔警冲突调查：韩警平均每天扣1艘中国渔船"}
'

curl -XPUT http://localhost:${ES_HTTP_PORT}/${TEST_IK_INDEX_NAME}/_doc/4 -H 'Content-Type:application/json' -d'
{"content":"中国驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首"}
'

# 4. query with highlighting

sleep 1

curl -XPOST http://localhost:${ES_HTTP_PORT}/${TEST_IK_INDEX_NAME}/_search  -H 'Content-Type:application/json' -d'
{
    "query" : { "match" : { "content" : "中国" }},
    "highlight" : {
        "pre_tags" : ["<tag1>", "<tag2>"],
        "post_tags" : ["</tag1>", "</tag2>"],
        "fields" : {
            "content" : {}
        }
    }
}
'

# Result
#
# {
#     "took": 14,
#     "timed_out": false,
#     "_shards": {
#         "total": 5,
#         "successful": 5,
#         "failed": 0
#     },
#     "hits": {
#         "total": 2,
#         "max_score": 2,
#         "hits": [
#             {
#                 "_index": "index",
#                 "_type": "fulltext",
#                 "_id": "4",
#                 "_score": 2,
#                 "_source": {
#                     "content": "中国驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首"
#                 },
#                 "highlight": {
#                     "content": [
#                         "<tag1>中国</tag1>驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首 "
#                     ]
#                 }
#             },
#             {
#                 "_index": "index",
#                 "_type": "fulltext",
#                 "_id": "3",
#                 "_score": 2,
#                 "_source": {
#                     "content": "中韩渔警冲突调查：韩警平均每天扣1艘中国渔船"
#                 },
#                 "highlight": {
#                     "content": [
#                         "均每天扣1艘<tag1>中国</tag1>渔船 "
#                     ]
#                 }
#             }
#         ]
#     }
# }
