package org.graylog.plugins.views.startpage.lastOpened;

import org.joda.time.DateTime;

public record Item(String id, DateTime timestamp) {
}
