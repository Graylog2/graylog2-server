package org.graylog2.contentpacks;

import org.graylog.security.UserContext;
import org.graylog.security.shares.EntityShareRequest;
import org.graylog2.contentpacks.model.ContentPackInstallation;

public interface ContentPackInstallationHook {
    void afterInstallation(ContentPackInstallation installation, EntityShareRequest shareRequest, UserContext userContext);
}
