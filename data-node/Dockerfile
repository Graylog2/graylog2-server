FROM eclipse-temurin:17-jre-jammy

ARG DATANODE_HOME=/usr/share/graylog/datanode
ARG OPENSEARCH_VERSION=2.4.1

WORKDIR ${DATANODE_HOME}

ADD target/datanode-5.1.0-SNAPSHOT.jar .
ADD target/lib lib/

ADD bin/opensearch-${OPENSEARCH_VERSION} opensearch-${OPENSEARCH_VERSION}/
# ADD https://artifacts.opensearch.org/releases/bundle/opensearch/${OPENSEARCH_VERSION}/opensearch-${OPENSEARCH_VERSION}-linux-x64.tar.gz opensearch-${OPENSEARCH_VERSION}/

ADD datanode.conf .

RUN mkdir -p config
RUN mkdir -p data
RUN mkdir -p logs

RUN useradd opensearch
RUN chown -R opensearch:opensearch ${DATANODE_HOME}
USER opensearch

EXPOSE 8999

ENTRYPOINT ["java", "-jar", "datanode-5.1.0-SNAPSHOT.jar", "datanode", "-f", "datanode.conf"]
