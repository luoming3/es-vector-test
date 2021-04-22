help:  # list all targets
	@grep ^[a-z] Makefile

XDOC_IMAGE_REPOSITORY = open-distro-es
XDOC_IMAGE_TAG = 1.0

ES_IP1 = 192.168.1.2
ES_IP2 = 192.168.1.3
ES_IP3 = 192.168.1.4
M1_PORT = 9200
M1_TCP_PORT = 9300
D1_PORT = 9210
D1_TCP_PORT = 9310
ES_DIR = ~/elasticsearch-cluster/

# backup server ip
BACKUP_SERVER_IP = 192.168.1.1
# dir in backup server
ES_BACKUP_DIR = data/es/backup
# dir in work server
ES_BACKUP_DATA = ~/data/es/snapshot

####### END-OF-TEMPLATE

pull-elasticsearch-oss:  # pull the image
	docker pull docker.elastic.co/elasticsearch/elasticsearch-oss:7.9.3

build-open-distro-elsticsearch: prerequisite # install plugins for elasticsearch
	docker build --tag ${XDOC_IMAGE_REPOSITORY}:${XDOC_IMAGE_TAG} .

pull-kibana-oss:  # pull the image
	docker pull docker.elastic.co/kibana/kibana-oss:7.9.3

run-distro-elasticsearch-single:
	docker run -d --name=open-distro-elasticsearch-single -p 9240:9200 -p 9640:9600 \
	-e "discovery.type=single-node" \
	-v $(shell pwd)/elasticsearch-single.yml:/usr/share/elasticsearch/config/elasticsearch.yml \
	${XDOC_IMAGE_REPOSITORY}:${XDOC_IMAGE_TAG}

run-distro-elasticsearch-cluster: prerequisite  # 3 nodes
	docker-compose up -d

stop-distro-elasticsearch-cluster:
	docker-compose -f docker-compose.yml down

prerequisite:
	@echo "Curentt ES_XDOC_VERSION=${XDOC_IMAGE_TAG}"
	@echo
	@if [ -z "${XDOC_IMAGE_TAG}" ]; \
	then \
		echo "Abort! Please set ES_XDOC_VERSION first via \n \
		export ES_XDOC_VERSION='elasticsearch-x-doc-search version'"; \
		exit 1; \
	fi

	# max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
	sysctl -w vm.max_map_count=262144


##### on multiple machines

install-sshfs-cluster:
	set -e ; \
	for IP in ${BACKUP_SERVER_IP} ${ES_IP1} ${ES_IP2} ${ES_IP3} ; do \
		echo $${IP} ; \
		ssh $${IP} "sudo apt-get install sshfs" ; \
		done ;

create-share-file:
	set -ex ; \
	ssh ${BACKUP_SERVER_IP} "mkdir -p ~/${ES_BACKUP_DIR}; chmod -R 777 ~/${ES_BACKUP_DIR}" ; \
	for IP in ${ES_IP1} ${ES_IP2} ${ES_IP3} ; do \
		echo $${IP} ; \
		ssh $${IP} "mkdir -p ${ES_BACKUP_DATA}; chmod -R 777 ${ES_BACKUP_DATA}; \
			sshfs -o allow_other ${BACKUP_SERVER_IP}:${ES_BACKUP_DIR} ${ES_BACKUP_DATA}" ; \
	done

clean-share-file:
	set -ex ; \
	for IP in ${ES_IP1} ${ES_IP2} ${ES_IP3} ; do \
    		echo $${IP} ; \
    		ssh $${IP} "fusermount -u ${ES_BACKUP_DATA}; rm -rf ${ES_BACKUP_DATA}" ; \
	done ; \
	ssh ${BACKUP_SERVER_IP} "rm -rf ~/${ES_BACKUP_DIR}"

clean-data-volume:
	set -ex ; \
	for IP in ${ES_IP1} ${ES_IP2} ${ES_IP3} ; do \
			echo $${IP} ; \
			ssh $${IP} "docker volume rm esmaster1 esdata1" ; \
	done

scp-elasticsearch-to-target-machines:
	set -e ; \
	for IP in ${ES_IP1} ${ES_IP2} ${ES_IP3} ; do \
		echo $${IP} ; \
		ssh $${IP} "mkdir -p ${ES_DIR}" ; \
		scp -r cluster/* Dockerfile Makefile IKAnalyzer.cfg.xml dynamic-synonym $${IP}:${ES_DIR} ; \
	done

build-elasticsearch-x-doc-search-cluster:
	set -e ; \
	for IP in ${ES_IP1} ${ES_IP2} ${ES_IP3} ; do \
		echo $${IP} ; \
		ssh $${IP} "cd ${ES_DIR}; make build-elasticsearch-x-doc-search" ; \
	done

run-elasticsearch-cluster: 
	set -e ; \
	for IP in ${ES_IP1} ${ES_IP2} ${ES_IP3} ; do \
		echo $${IP} ; \
		ssh $${IP} "export XDOC_IMAGE_REPOSITORY=${XDOC_IMAGE_REPOSITORY} XDOC_IMAGE_TAG=${XDOC_IMAGE_TAG} IP1=${ES_IP1} IP2=${ES_IP2} IP3=${ES_IP3} IP=$${IP} M1_PORT=${M1_PORT} M1_TCP_PORT=${M1_TCP_PORT} D1_PORT=${D1_PORT} D1_TCP_PORT=${D1_TCP_PORT} ES_BACKUP_DATA=${ES_BACKUP_DATA}; \
			sudo sysctl -w vm.max_map_count=262144; \
			cd ${ES_DIR}; docker-compose up -d" ; \
    done

stop-elasticsearch-cluster-and-remove-containers:
	set -e ; \
	for IP in ${ES_IP1} ${ES_IP2} ${ES_IP3} ; do \
		echo $${IP} ; \
		ssh $${IP} "export XDOC_IMAGE_REPOSITORY=${XDOC_IMAGE_REPOSITORY} XDOC_IMAGE_TAG=${XDOC_IMAGE_TAG} IP1=${ES_IP1} IP2=${ES_IP2} IP3=${ES_IP3} IP=$${IP} M1_PORT=${M1_PORT} M1_TCP_PORT=${M1_TCP_PORT} D1_PORT=${D1_PORT} D1_TCP_PORT=${D1_TCP_PORT}; \
			cd ${ES_DIR}; docker-compose down" ; \
    done
