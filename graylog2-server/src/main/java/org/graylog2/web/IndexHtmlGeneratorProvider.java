package org.graylog2.web;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class IndexHtmlGeneratorProvider implements Provider<IndexHtmlGenerator> {
    private final Provider<? extends IndexHtmlGenerator> indexHtmlGeneratorProvider;

    @Inject
    public IndexHtmlGeneratorProvider(Provider<DevelopmentIndexHtmlGenerator> developmentIndexHtmlGeneratorProvider,
                                      Provider<ProductionIndexHtmlGenerator> productionIndexHtmlGeneratorProvider,
                                      @Named("isDevelopmentServer") Boolean isDevelopmentServer) {
        this.indexHtmlGeneratorProvider = isDevelopmentServer
                ? developmentIndexHtmlGeneratorProvider
                : productionIndexHtmlGeneratorProvider;
    }

    @Override
    public IndexHtmlGenerator get() {
        return indexHtmlGeneratorProvider.get();
    }
}
