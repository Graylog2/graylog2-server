/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.sidecar.services;

import com.mongodb.BasicDBObject;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.template.directives.IndentTemplateDirective;
import org.graylog.plugins.sidecar.template.loader.MongoDbTemplateLoader;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ConfigurationService extends PaginatedDbService<Configuration> {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);
    private static final freemarker.template.Configuration templateConfiguration =
            new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_28);
    private static final StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();

    private static final String COLLECTION_NAME = "sidecar_configurations";

    @Inject
    public ConfigurationService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, Configuration.class, COLLECTION_NAME);
        MongoDbTemplateLoader mongoDbTemplateLoader = new MongoDbTemplateLoader(db);
        MultiTemplateLoader multiTemplateLoader = new MultiTemplateLoader(new TemplateLoader[] {
                mongoDbTemplateLoader,
                stringTemplateLoader });
        templateConfiguration.setTemplateLoader(multiTemplateLoader);
        templateConfiguration.setSharedVariable("indent", new IndentTemplateDirective());
    }

    public Configuration find(String id) {
        return db.findOne(DBQuery.is("_id", id));
    }

    public Configuration findByName(String name) {
        return db.findOne(DBQuery.is(Configuration.FIELD_NAME, name));
    }

    public List<Configuration> all() {
        try (final Stream<Configuration> collectorConfigurationStream = streamAll()) {
            return collectorConfigurationStream.collect(Collectors.toList());
        }
    }

    public PaginatedList<Configuration> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    @Override
    public Configuration save(Configuration configuration) {
        return db.findAndModify(DBQuery.is("_id", configuration.id()), new BasicDBObject(),
                new BasicDBObject(), false, configuration, true, true);
    }

    public Configuration copyConfiguration(String id, String name) {
        Configuration configuration = find(id);
        return Configuration.create(configuration.collectorId(), name, configuration.color(), configuration.template());
    }

    public Configuration fromRequest(Configuration request) {
        return Configuration.create(
                request.collectorId(),
                request.name(),
                request.color(),
                request.template());
    }

    public Configuration fromRequest(String id, Configuration request) {
        return Configuration.create(
                id,
                request.collectorId(),
                request.name(),
                request.color(),
                request.template());
    }

    public Configuration renderConfigurationForCollector(Sidecar sidecar, Configuration configuration) {
        Map<String, Object> context = new HashMap<>();

        context.put("nodeId", sidecar.nodeId());
        context.put("nodeName", sidecar.nodeName());
        context.put("sidecarVersion", sidecar.sidecarVersion());
        context.put("operatingSystem", sidecar.nodeDetails().operatingSystem());
        context.put("ip", sidecar.nodeDetails().ip());
        if (sidecar.nodeDetails().metrics().cpuIdle() != null) {
            context.put("cpuIdle", sidecar.nodeDetails().metrics().cpuIdle());
        }
        if (sidecar.nodeDetails().metrics().load1() != null) {
            context.put("load1", sidecar.nodeDetails().metrics().load1());
        }

        return Configuration.create(
                configuration.id(),
                configuration.collectorId(),
                configuration.name(),
                configuration.color(),
                renderTemplate(configuration.id(), context)
        );
    }

    public String renderPreview(String template) {
        Map<String, Object> context = new HashMap<>();
        context.put("nodeId", "<node id>");
        context.put("nodeName", "<node name>");
        context.put("sidecarVersion", "<version>");
        context.put("operatingSystem", "<operating system>");
        context.put("ip", "<ip>");
        context.put("cpuIdle", "<cpu idle>");
        context.put("load1", "<load 1>");

        String previewName = UUID.randomUUID().toString();
        stringTemplateLoader.putTemplate(previewName, template);
        String result = renderTemplate(previewName, context);
        stringTemplateLoader.removeTemplate(previewName);
        try {
            templateConfiguration.removeTemplateFromCache(previewName);
        } catch (IOException e) {
            LOG.debug("Couldn't remove temporary template from cache: " + e.getMessage());
        }

        return result;
    }

    private String renderTemplate(String configurationId, Map<String, Object> context) {
        Writer writer = new StringWriter();
        try {
            Template compiledTemplate = templateConfiguration.getTemplate(configurationId);
            compiledTemplate.process(context, writer);
        } catch (TemplateException | IOException e) {
            LOG.error("Failed to render template: ", e);
            return null;
        }

        return writer.toString();
    }
}
