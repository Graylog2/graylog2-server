/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.sidecar.template.directives;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

public class IndentTemplateDirective implements TemplateDirectiveModel {
    private static final String PARAM_NAME_COUNT = "count";

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws
            TemplateException, IOException {
        int countParam = 0;

        // Check if no parameters were given:
        if (params.size() != 1) {
            throw new TemplateModelException("Provide 'count' parameter to use the @indent directive.");
        }
        if (loopVars.length != 0) {
            throw new TemplateModelException("This directive doesn't allow loop variables.");
        }

        for (Object entry : params.entrySet()) {
            Map.Entry parameter = (Map.Entry) entry;

            String paramName = (String) parameter.getKey();
            TemplateModel paramValue = (TemplateModel) parameter.getValue();

            if (paramName.equals(PARAM_NAME_COUNT)) {
                if (!(paramValue instanceof TemplateNumberModel)) {
                    throw new TemplateModelException("Parameter '" + PARAM_NAME_COUNT + "' must be a number.");
                }
                countParam = ((TemplateNumberModel) paramValue).getAsNumber().intValue();
                if (countParam < 0) {
                    throw new TemplateModelException("Parameter '" + PARAM_NAME_COUNT + "' can't be negative.");
                }
            }
        }

        // If there is non-empty nested content:
        if (body != null) {
            // Executes the nested body. Same as <#nested> in FTL, except
            // that we use our own writer instead of the current output writer.
            body.render(new IndentFilterWriter(countParam, env.getOut()));
        } else {
            throw new RuntimeException("Body is missing");
        }
    }

    private static class IndentFilterWriter extends Writer {

        private final int count;
        private final Writer out;

        IndentFilterWriter (int count, Writer out) {
            this.count = count;
            this.out = out;
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            String sbuf = String.valueOf(cbuf);
            sbuf = sbuf.replaceAll("\n",
                    "\n" + String.join("", Collections.nCopies(count, " ")));
            out.write(sbuf);
        }

        public void flush() throws IOException {
            out.flush();
        }

        public void close() throws IOException {
            out.close();
        }
    }

}
