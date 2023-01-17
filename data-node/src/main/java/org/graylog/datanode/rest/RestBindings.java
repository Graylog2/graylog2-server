package org.graylog.datanode.rest;

import org.graylog2.plugin.inject.Graylog2Module;

public class RestBindings extends Graylog2Module {
    @Override
    protected void configure() {
        addSystemRestResource(StatusController.class);
        addSystemRestResource(LogsController.class);
    }
}
