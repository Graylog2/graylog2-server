package org.graylog.events.processor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class SystemEntityActionService {

    public Optional<String> findActionDeniedError(SystemEntity entity, SystemEntityAction action) {

        if (hasDeniedAction(entity, action)) {
            String error = String.format(Locale.ENGLISH, "Action '%s' is not permitted for '%s(%s)", action, entity.getClass(), entity.id());
            return Optional.of(error);
        }

        return Optional.empty();

    }

    public boolean isViewable(SystemEntity entity) {
        return !hasDeniedAction(entity, SystemEntityAction.VIEW);
    }

    public boolean isExportable(SystemEntity entity) {
        return !hasDeniedAction(entity, SystemEntityAction.EXPORT);
    }

    private boolean hasDeniedAction(SystemEntity entity, SystemEntityAction action) {
        if (entity == null || entity.deniedActions() == null) {
            return false;
        }
        return entity.deniedActions().contains(action);
    }

    public <T extends SystemEntity> List<T> filterViewable(List<T> original) {
        return original.stream()
                .filter(this::isViewable)
                .collect(Collectors.toList());
    }

    public <T extends SystemEntity> List<T> filterExportable(List<T> original) {
        return original.stream()
                .filter(this::isExportable)
                .collect(Collectors.toList());
    }
}
