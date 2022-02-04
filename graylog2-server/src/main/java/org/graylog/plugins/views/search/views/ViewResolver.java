package org.graylog.plugins.views.search.views;

import java.util.Optional;

/**
 * A generic View resolver interface, which provides the ability to look a view up from an alternate source.
 */
public interface ViewResolver {
    Optional<ViewDTO> get(String id);
}
