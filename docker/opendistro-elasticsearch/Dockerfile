FROM amazon/opendistro-for-elasticsearch:1.11.0

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin remove opendistro_security

COPY --chown=elasticsearch:elasticsearch elasticsearch.yml /usr/share/elasticsearch/config/

# https://github.com/medcl/elasticsearch-analysis-ik
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch \
    https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.9.1/elasticsearch-analysis-ik-7.9.1.zip

# https://www.elastic.co/guide/en/elasticsearch/plugins/current/repository-hdfs.html
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch repository-hdfs

# Fix http://10.127.1.171:8888/lenet/server/issues/335
# ElasticSearchException: failed to create blob container
#
# Refer to https://www.elastic.co/guide/en/elasticsearch/reference/7.9/docker.html#_notes_for_production_use_and_defaults
RUN mkdir /tmp/es-bak && chmod g+rwx /tmp/es-bak && chgrp 0 /tmp/es-bak
