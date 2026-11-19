/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import org.graylog2.datanode.repositories.RepositoryClientNames;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The snapshot repositories of the opensearch cluster and the repository clients its nodes know. Opensearch doesn't
 * reveal secure settings, so a client configured only in the keystore isn't listed. Graylog gives each client it
 * manages at least one regular setting, so these are always listed.
 *
 * @param repositories the registered snapshot repositories
 * @param clients      the clients, one entry per distinct configuration, e.g. a client whose settings changed on
 *                     some nodes only appears once for every configuration
 */
public record RepositoryClientsResponse(@JsonProperty("repositories") List<Repository> repositories,
                                        @JsonProperty("clients") List<Client> clients) {

    private static final Set<String> CLIENT_TYPES = Set.of("s3", "gcs", "azure");
    private static final Pattern CLIENT_SETTING = Pattern.compile("(s3|gcs|azure)\\.client\\.([^.]+)\\.(.+)");
    private static final String DEFAULT_CLIENT = "default";

    /**
     * @param client              the client of a repository type with clients, {@code null} for other types
     * @param nodesWithoutClient  the nodes that don't know the client yet, e.g. because they haven't restarted since
     *                            it was created. {@code null} for clients Graylog doesn't manage, they may be
     *                            configured in the keystore only.
     */
    public record Repository(@JsonProperty("name") String name,
                             @JsonProperty("type") String type,
                             @JsonProperty("client") @Nullable String client,
                             @JsonProperty("nodes_without_client") @Nullable List<String> nodesWithoutClient) {}

    /**
     * @param settings the regular settings of the client, relative to it
     * @param nodes    the nodes with this configuration of the client
     */
    public record Client(@JsonProperty("type") String type,
                         @JsonProperty("name") String name,
                         @JsonProperty("settings") Map<String, String> settings,
                         @JsonProperty("nodes") List<String> nodes) {}

    private record ClientKey(String type, String name) {}

    /**
     * @param repositories the response of {@code GET _snapshot}
     * @param nodes        the response of {@code GET _nodes/settings?flat_settings=true}
     */
    public static RepositoryClientsResponse from(JsonNode repositories, JsonNode nodes) {
        // client -> node -> settings of the client on that node
        final Map<ClientKey, Map<String, Map<String, String>>> clientsByNode = new TreeMap<>(
                Comparator.comparing(ClientKey::type).thenComparing(ClientKey::name));
        final Set<String> nodeNames = new TreeSet<>();
        nodes.path("nodes").forEach(node -> {
            final String nodeName = node.path("name").asText();
            nodeNames.add(nodeName);
            node.path("settings").properties().forEach(setting -> {
                final Matcher matcher = CLIENT_SETTING.matcher(setting.getKey());
                if (matcher.matches()) {
                    clientsByNode.computeIfAbsent(new ClientKey(matcher.group(1), matcher.group(2)), key -> new TreeMap<>())
                            .computeIfAbsent(nodeName, name -> new TreeMap<>())
                            .put(matcher.group(3), setting.getValue().asText());
                }
            });
        });

        final List<Client> clients = new ArrayList<>();
        clientsByNode.forEach((key, settingsByNode) -> {
            final Map<Map<String, String>, List<String>> nodesBySettings = new LinkedHashMap<>();
            settingsByNode.forEach((node, settings) -> nodesBySettings.computeIfAbsent(settings, s -> new ArrayList<>()).add(node));
            nodesBySettings.forEach((settings, nodesWithSettings) -> clients.add(new Client(key.type(), key.name(), settings, nodesWithSettings)));
        });

        final List<Repository> repositoryList = new ArrayList<>();
        repositories.properties().forEach(repository -> {
            final String type = repository.getValue().path("type").asText();
            if (!CLIENT_TYPES.contains(type)) {
                repositoryList.add(new Repository(repository.getKey(), type, null, null));
                return;
            }
            final String client = repository.getValue().path("settings").path("client").asText(DEFAULT_CLIENT);
            final List<String> nodesWithoutClient = RepositoryClientNames.isManaged(client)
                    ? nodeNames.stream()
                    .filter(node -> !clientsByNode.getOrDefault(new ClientKey(type, client), Map.of()).containsKey(node))
                    .toList()
                    : null;
            repositoryList.add(new Repository(repository.getKey(), type, client, nodesWithoutClient));
        });
        repositoryList.sort(Comparator.comparing(Repository::name));
        return new RepositoryClientsResponse(repositoryList, clients);
    }
}
