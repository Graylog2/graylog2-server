package org.graylog.testing.datanode;


import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public interface DatanodeDevContainerBuilder {
    DatanodeDevContainerBuilder mongoDbUri(final String mongoDbUri);
    DatanodeDevContainerBuilder passwordSecret(final String passwordSecret);
    DatanodeDevContainerBuilder rootPasswordSha2(final String rootPasswordSha2);

    DatanodeDevContainerBuilder restPort(int restPort);

    DatanodeDevContainerBuilder openSearchPort(int openSearchPort);

    DatanodeDevContainerBuilder nodeName(String nodeName);

    DatanodeDevContainerBuilder customizer(DatanodeDockerHooks hooks);

    DatanodeDevContainerBuilder network(Network network);

    GenericContainer<?> build();
}
