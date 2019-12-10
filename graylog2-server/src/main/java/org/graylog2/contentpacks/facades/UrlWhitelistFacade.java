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
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.Version;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.graylog2.system.urlwhitelist.WhitelistEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.contentpacks.model.ModelTypes.URL_WHITELIST_ENTRY_V1;

public class UrlWhitelistFacade implements EntityFacade<WhitelistEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(UrlWhitelistFacade.class);

    public static final ModelType TYPE_V1 = URL_WHITELIST_ENTRY_V1;

    private final ObjectMapper objectMapper;
    private final UrlWhitelistService urlWhitelistService;

    @Inject
    public UrlWhitelistFacade(ObjectMapper objectMapper, UrlWhitelistService urlWhitelistService) {
        this.objectMapper = objectMapper;
        this.urlWhitelistService = urlWhitelistService;
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();

        return urlWhitelistService.getEntry(modelId.id())
                .map(entry -> EntityV1.builder()
                        .id(ModelId.of(entityDescriptorIds.getOrThrow(entry.id(), URL_WHITELIST_ENTRY_V1)))
                        .type(URL_WHITELIST_ENTRY_V1)
                        .data(objectMapper.convertValue(entry, JsonNode.class))
                        .constraints(ImmutableSet.of(GraylogVersionConstraint.of(Version.from(3, 1, 3))))
                        .build());
    }

    @Override
    public NativeEntity<WhitelistEntry> createNativeEntity(Entity entity, Map<String, ValueReference> parameters,
            Map<EntityDescriptor, Object> nativeEntities, String username) {

        if (!(entity instanceof EntityV1)) {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }

        final WhitelistEntry whitelistEntry =
                objectMapper.convertValue(((EntityV1) entity).data(), WhitelistEntry.class);

        urlWhitelistService.addEntry(whitelistEntry);

        return NativeEntity.create(entity.id(), whitelistEntry.id(), TYPE_V1, createTitle(whitelistEntry),
                whitelistEntry);
    }

    @Override
    public Optional<NativeEntity<WhitelistEntry>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        final ModelId modelId = nativeEntityDescriptor.id();
        return urlWhitelistService.getEntry(modelId.id())
                .map(entry -> NativeEntity.create(nativeEntityDescriptor, entry));
    }

    @Override
    public void delete(WhitelistEntry nativeEntity) {
        urlWhitelistService.removeEntry(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(WhitelistEntry whitelistEntry) {
        return EntityExcerpt.builder()
                .id(ModelId.of(whitelistEntry.id()))
                .type(URL_WHITELIST_ENTRY_V1)
                .title(createTitle(whitelistEntry))
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return urlWhitelistService.get()
                .entries()
                .stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    private String createTitle(WhitelistEntry entry) {
        return entry.title()
                .map(t -> t + " [" + entry.value() + "]")
                .orElse(entry.value());
    }
}
