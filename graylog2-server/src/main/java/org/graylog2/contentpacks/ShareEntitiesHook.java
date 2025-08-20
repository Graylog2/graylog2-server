package org.graylog2.contentpacks;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.UserContext;
import org.graylog.security.shares.EntityShareRequest;
import org.graylog.security.shares.EntitySharesService;
import org.graylog2.contentpacks.model.ContentPackInstallation;

public class ShareEntitiesHook implements ContentPackInstallationHook {
    private final GRNRegistry grnRegistry;
    private final Provider<EntitySharesService> entitySharesService;

    @Inject
    public ShareEntitiesHook(GRNRegistry grnRegistry, Provider<EntitySharesService> entitySharesService) {
        this.grnRegistry = grnRegistry;
        this.entitySharesService = entitySharesService;
    }

    @Override
    public void afterInstallation(ContentPackInstallation installation, EntityShareRequest shareRequest, UserContext userContext) {
        final var user = userContext.getUser();
        final var allEntities = installation.entities();
        final var entityGRNs = allEntities.stream()
                .map(entity -> grnRegistry.newGRN(entity.type().name(), entity.id().id()))
                .toList();
        entityGRNs.forEach((grn) -> entitySharesService.get().updateEntityShares(grn, shareRequest, user));
    }
}
