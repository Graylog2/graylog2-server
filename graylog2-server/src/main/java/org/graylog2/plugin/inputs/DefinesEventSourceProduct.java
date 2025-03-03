package org.graylog2.plugin.inputs;

public interface DefinesEventSourceProduct {
    /**
     * Every codec except the general Syslog/GELF codecs set this attribute and we start to depend on
     * it in code, so we make this a mandatory getter
     * @return
     */
    String getEventSourceProduct();
}
