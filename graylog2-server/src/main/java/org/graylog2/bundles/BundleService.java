/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bundles;

import com.google.common.collect.Iterators;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.BundleExporterProvider;
import org.graylog2.bindings.providers.BundleImporterProvider;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.users.User;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class BundleService {
    private static final Logger LOG = LoggerFactory.getLogger(BundleService.class);
    private static final String COLLECTION_NAME = "content_packs";

    private final JacksonDBCollection<ConfigurationBundle, ObjectId> dbCollection;
    private final BundleImporterProvider bundleImporterProvider;
    private final BundleExporterProvider bundleExporterProvider;

    @Inject
    public BundleService(
            final MongoJackObjectMapperProvider mapperProvider,
            final MongoConnection mongoConnection,
            final BundleImporterProvider bundleImporterProvider,
            final BundleExporterProvider bundleExporterProvider) {
        this(JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                        ConfigurationBundle.class, ObjectId.class, mapperProvider.get()),
                bundleImporterProvider, bundleExporterProvider);
    }

    public BundleService(final JacksonDBCollection<ConfigurationBundle, ObjectId> dbCollection,
                         final BundleImporterProvider bundleImporterProvider,
                         final BundleExporterProvider bundleExporterProvider) {
        this.dbCollection = dbCollection;
        this.bundleImporterProvider = bundleImporterProvider;
        this.bundleExporterProvider = bundleExporterProvider;
    }

    public ConfigurationBundle load(final String bundleId) throws NotFoundException {
        final ConfigurationBundle bundle = dbCollection.findOneById(new ObjectId(bundleId));

        if (bundle == null) {
            throw new NotFoundException();
        }

        return bundle;
    }

    public ConfigurationBundle findByNameAndCategory(final String name, final String category) {
        final DBQuery.Query query = DBQuery.is("name", name).is("category", category);
        final ConfigurationBundle bundle = dbCollection.findOne(query);

        return bundle;
    }

    public Set<ConfigurationBundle> loadAll() {
        final DBCursor<ConfigurationBundle> ConfigurationBundles = dbCollection.find();
        final Set<ConfigurationBundle> bundles = new HashSet<>();

        if (ConfigurationBundles.hasNext()) {
            Iterators.addAll(bundles, ConfigurationBundles);
        }

        return bundles;
    }

    public ConfigurationBundle update(final String bundleId, final ConfigurationBundle bundle) {
        final WriteResult<ConfigurationBundle, ObjectId> writeResult = dbCollection.updateById(new ObjectId(bundleId), bundle);
        return writeResult.getSavedObject();
    }

    public ConfigurationBundle insert(final ConfigurationBundle bundle) {
        final WriteResult<ConfigurationBundle, ObjectId> writeResult = dbCollection.insert(bundle);
        return writeResult.getSavedObject();
    }

    public int delete(String bundleId) {
        return dbCollection.removeById(new ObjectId(bundleId)).getN();
    }

    public void applyConfigurationBundle(final String bundleId, User actingUser) throws NotFoundException {
        applyConfigurationBundle(load(bundleId), actingUser);
    }

    public void applyConfigurationBundle(final ConfigurationBundle bundle, User actingUser) {
        final String userName = actingUser.getName();

        final BundleImporter bundleImporter = bundleImporterProvider.get();
        bundleImporter.runImport(bundle, userName);
    }

    public ConfigurationBundle exportConfigurationBundle(final ExportBundle exportBundle) {
        final BundleExporter bundleExporter = bundleExporterProvider.get();
        return bundleExporter.export(exportBundle);
    }
}