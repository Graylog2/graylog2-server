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
package org.graylog2.contentpacks;

import org.graylog2.contentpacks.catalogs.CatalogIndex;
import org.graylog2.contentpacks.constraints.ConstraintChecker;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

@Singleton
public class ContentPackService {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackService.class);

    private final CatalogIndex catalogIndex;
    private final ContentPackPersistenceService contentPackPersistenceService;
    private final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;
    private final Set<ConstraintChecker> constraintCheckers;

    @Inject
    public ContentPackService(CatalogIndex catalogIndex,
                              ContentPackPersistenceService contentPackPersistenceService,
                              ContentPackInstallationPersistenceService contentPackInstallationPersistenceService,
                              Set<ConstraintChecker> constraintCheckers) {
        this.catalogIndex = catalogIndex;
        this.contentPackPersistenceService = contentPackPersistenceService;
        this.contentPackInstallationPersistenceService = contentPackInstallationPersistenceService;
        this.constraintCheckers = constraintCheckers;
    }

    public ContentPackInstallation installContentPack(ContentPack contentPack,
                                                      Map<String, ValueReference> parameters,
                                                      String comment,
                                                      String user) {
        throw new UnsupportedOperationException();
    }
}
