FROM docker.elastic.co/elasticsearch/elasticsearch-oss:7.9.3

# https://github.com/medcl/elasticsearch-analysis-ik
RUN ./bin/elasticsearch-plugin install --batch \
    https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.9.3/elasticsearch-analysis-ik-7.9.3.zip

# https://www.elastic.co/guide/en/elasticsearch/plugins/current/repository-hdfs.html
RUN ./bin/elasticsearch-plugin install --batch repository-hdfs

RUN elasticsearch-plugin install --batch https://github.com/alexklibisz/elastiknn/releases/download/0.1.0-PRE51/elastiknn-0.1.0-PRE51_es7.9.3.zip

# Fix http://10.127.1.171:8888/lenet/server/issues/335
# ElasticSearchException: failed to create blob container
#
# Refer to https://www.elastic.co/guide/en/elasticsearch/reference/7.9/docker.html#_notes_for_production_use_and_defaults
RUN mkdir /tmp/es-bak && chmod g+rwx /tmp/es-bak && chown 1000:0 /tmp/es-bak
