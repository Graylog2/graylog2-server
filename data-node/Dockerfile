FROM eclipse-temurin:17-jre-jammy

ARG DATANODE_HOME=/usr/share/graylog/datanode

WORKDIR ${DATANODE_HOME}

ADD target/datanode-5.1.0-SNAPSHOT.jar .
ADD target/lib lib/
ADD bin/opensearch-2.4.1 opensearch-2.4.1/
ADD datanode.conf .

RUN mkdir -p config
RUN mkdir -p data
RUN mkdir -p logs

RUN useradd opensearch
RUN chown -R opensearch:opensearch ${DATANODE_HOME}
USER opensearch

RUN ls -lah  /usr/share/graylog/datanode/opensearch-2.4.1/config/opensearch.yml

EXPOSE 8999

ENTRYPOINT ["java", "-jar", "datanode-5.1.0-SNAPSHOT.jar", "datanode", "-f", "datanode.conf"]
