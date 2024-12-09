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
package org.graylog.plugins.sidecar.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.ConfigurationVariable;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.template.RenderTemplateException;
import org.graylog.plugins.sidecar.template.directives.IndentTemplateDirective;
import org.graylog.plugins.sidecar.template.loader.MongoDbTemplateLoader;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Indexes.ascending;
import static org.apache.commons.lang.CharEncoding.UTF_8;
import static org.graylog2.database.utils.MongoUtils.idEq;

@Singleton
public class ConfigurationService {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);
    private final freemarker.template.Configuration templateConfiguration;
    private final ConfigurationVariableService configurationVariableService;

    private static final String COLLECTION_NAME = "sidecar_configurations";
    private final Provider<freemarker.template.Configuration> templateConfigurationProvider;
    private final MongoCollection<Configuration> collection;
    private final MongoPaginationHelper<Configuration> paginationHelper;
    private final MongoUtils<Configuration> mongoUtils;

    @Inject
    public ConfigurationService(MongoCollections mongoCollections,
                                ConfigurationVariableService configurationVariableService,
                                Provider<freemarker.template.Configuration> templateConfigurationProvider) {
        this.templateConfigurationProvider = templateConfigurationProvider;
        this.configurationVariableService = configurationVariableService;

        collection = mongoCollections.collection(COLLECTION_NAME, Configuration.class);
        paginationHelper = mongoCollections.paginationHelper(collection);
        mongoUtils = mongoCollections.utils(collection);

        templateConfiguration = createTemplateConfiguration(new MongoDbTemplateLoader(mongoUtils));

        collection.createIndex(ascending(Configuration.FIELD_ID));
        collection.createIndex(ascending(Configuration.FIELD_COLLECTOR_ID));
        collection.createIndex(ascending(Configuration.FIELD_TAGS));
    }

    private freemarker.template.Configuration createTemplateConfiguration(TemplateLoader templateLoader) {
        final freemarker.template.Configuration configuration = templateConfigurationProvider.get();

        configuration.setTemplateLoader(templateLoader);
        configuration.setSharedVariable("indent", new IndentTemplateDirective());
        configuration.setDefaultEncoding(UTF_8);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);

        return configuration;
    }

    public Configuration find(String id) {
        return mongoUtils.getById(id).orElse(null);
    }

    public Configuration findByName(String name) {
        return collection.find(eq(Configuration.FIELD_NAME, name)).first();
    }

    public long count() {
        return collection.countDocuments();
    }

    public List<Configuration> all() {
        return collection.find().into(new ArrayList<>());
    }

    public PaginatedList<Configuration> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField,
                                                      SortOrder order) {
        return paginationHelper
                .filter(searchQuery.toBson())
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
    }

    public List<Configuration> findByConfigurationVariable(ConfigurationVariable configurationVariable) {
        final var filter = regex(
                Configuration.FIELD_TEMPLATE, Pattern.compile(Pattern.quote(configurationVariable.fullName())));
        return collection.find(filter).into(new ArrayList<>());
    }

    public List<Configuration> findByTags(Set<String> tags) {
        return collection.find(in(Configuration.FIELD_TAGS, tags)).into(new ArrayList<>());
    }

    public void replaceVariableNames(String oldName, String newName) {
        final var filter = regex(Configuration.FIELD_TEMPLATE, Pattern.compile(Pattern.quote(oldName)));
        collection.find(filter).forEach(config -> {
                    final String newTemplate = config.template().replace(oldName, newName);
                    collection.replaceOne(
                            idEq(Objects.requireNonNull(config.id())),
                            config.toBuilder().template(newTemplate).build(),
                            new ReplaceOptions().upsert(true)
                    );
                }
        );
    }

    public Configuration save(Configuration configuration) {
        return collection.findOneAndReplace(
                idEq(Objects.requireNonNull(configuration.id())),
                configuration,
                new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true)
        );
    }

    public Configuration copyConfiguration(String id, String name) {
        Configuration configuration = find(id);
        // Tags are not copied on purpose
        return Configuration.createWithoutId(configuration.collectorId(), name, configuration.color(), configuration.template(), Set.of());
    }

    public Configuration fromRequest(Configuration request) {
        return Configuration.createWithoutId(
                request.collectorId(),
                request.name(),
                request.color(),
                request.template(),
                request.tags());
    }

    public Configuration fromRequest(String id, Configuration request) {
        return Configuration.create(
                id,
                request.collectorId(),
                request.name(),
                request.color(),
                request.template(),
                request.tags());
    }

    public Configuration renderConfigurationForCollector(Sidecar sidecar, Configuration configuration) throws RenderTemplateException {
        Map<String, Object> context = new HashMap<>();

        context.put("nodeId", sidecar.nodeId());
        context.put("nodeName", sidecar.nodeName());
        context.put("sidecarVersion", sidecar.sidecarVersion());
        context.put("operatingSystem", sidecar.nodeDetails().operatingSystem());
        if (sidecar.nodeDetails().collectorConfigurationDirectory() != null) {
            String pathDelim = sidecar.nodeDetails().operatingSystem().equalsIgnoreCase("windows") ? "\\" : "/";
            context.put("spoolDir", sidecar.nodeDetails().collectorConfigurationDirectory() + pathDelim + configuration.id());
        }
        context.put("tags", sidecar.nodeDetails().tags().stream().collect(Collectors.toMap(t -> t, t -> true)));

        return Configuration.create(
                configuration.id(),
                configuration.collectorId(),
                configuration.name(),
                configuration.color(),
                renderTemplate(templateConfiguration, configuration.id(), context),
                configuration.tags()
        );
    }

    public String renderPreview(String template) throws RenderTemplateException {
        Map<String, Object> context = new HashMap<>();
        context.put("nodeId", "<node id>");
        context.put("nodeName", "<node name>");
        context.put("sidecarVersion", "<version>");
        context.put("operatingSystem", "<operating system>");
        context.put("spoolDir", "<sidecar spool directory>");
        context.put("tags", Map.of());

        String previewName = UUID.randomUUID().toString();
        final StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
        stringTemplateLoader.putTemplate(previewName, template);

        return renderTemplate(createTemplateConfiguration(stringTemplateLoader), previewName, context);
    }

    private String renderTemplate(freemarker.template.Configuration config, String templateName, Map<String, Object> sidecarContext) throws RenderTemplateException {
        Writer writer = new StringWriter();

        final Map<String, Object> context = new HashMap<>();
        context.put("sidecar", sidecarContext);

        final Map<String, Object> userContext =
                configurationVariableService.all().stream().collect(Collectors.toMap(ConfigurationVariable::name, ConfigurationVariable::content));
        context.put(ConfigurationVariable.VARIABLE_PREFIX, userContext);

        try {
            Template compiledTemplate = config.getTemplate(templateName);
            compiledTemplate.process(context, writer);
        } catch (TemplateException e) {
            LOG.error("Failed to render template: " + e.getMessageWithoutStackTop());
            throw new RenderTemplateException(e.getFTLInstructionStack(), e);
        } catch (IOException e) {
            LOG.error("Failed to render template: ", e);
            throw new RenderTemplateException(e.getMessage(), e);
        }

        final String template = writer.toString();
        return template.endsWith("\n") ? template : template + "\n";
    }

    public Optional<Configuration> get(String id) {
        return mongoUtils.getById(id);
    }

    public int delete(String id) {
        return mongoUtils.deleteById(id) ? 1 : 0;
    }
}
