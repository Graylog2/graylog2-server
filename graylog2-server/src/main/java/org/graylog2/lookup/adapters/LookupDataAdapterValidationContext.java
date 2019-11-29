package org.graylog2.lookup.adapters;

import com.google.inject.Inject;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

/**
 * Context object for configurations which require access to services to perform validation.
 */
public class LookupDataAdapterValidationContext {
    private final UrlWhitelistService urlWhitelistService;

    @Inject
    public LookupDataAdapterValidationContext(UrlWhitelistService urlWhitelistService) {
        this.urlWhitelistService = urlWhitelistService;
    }

    public UrlWhitelistService getUrlWhitelistService() {
        return urlWhitelistService;
    }
}
