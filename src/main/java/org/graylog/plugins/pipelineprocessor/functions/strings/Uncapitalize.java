package org.graylog.plugins.pipelineprocessor.functions.strings;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class Uncapitalize extends StringUtilsFunction {

    public static final String NAME = "uncapitalize";

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected boolean isLocaleAware() {
        return false;
    }

    @Override
    protected String apply(String value, Locale unused) {
        return StringUtils.uncapitalize(value);
    }
}
