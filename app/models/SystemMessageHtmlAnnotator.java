/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package models;

import com.google.common.collect.Lists;
import controllers.routes;

import java.util.ArrayList;
import java.util.List;

public class SystemMessageHtmlAnnotator {
    private final ArrayList<SystemMessage> annotatedMessages;

    public SystemMessageHtmlAnnotator(List<SystemMessage> messages) {
        this.annotatedMessages = Lists.newArrayList();
        for (SystemMessage systemMessage : messages) {
            this.annotatedMessages.add(new AnnotatedSystemMessage(systemMessage));
        }
    }

    public List<SystemMessage> getMessageList() {
        return annotatedMessages;
    }

    public class AnnotatedSystemMessage extends SystemMessage {
        public AnnotatedSystemMessage(SystemMessage sm) {
            super(sm);
        }

        @Override
        public String getContent() {
            String content = super.getContent();
            final String s = content.replaceAll("input <(.*?)>", "<a href=\"" + routes.InputsController.manage(getNodeId()) + "?input_id=$1\">input <$1></a>");
            return s;
        }
    }
}
