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
package org.graylog2.contentstream;

import com.google.inject.ImplementedBy;

import java.util.List;

@ImplementedBy(ContentStreamFeedTagsService.class)
public interface ContentStreamFeedTags {
    public enum FeedTags {
        OPEN("open-feed"),              //anyone on opensource
        ENTERPRISE("enterprise-feed"),  //anyone with Enterprise or Security License
        SMB("smb-feed");                //anyone with Small business free enterprise license (OPS only not security)

        public static final long SMB_TRAFFIC_LIMIT = 2L * 1024 * 1024 * 1024;

        private String tag;

        FeedTags(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }
    }

    abstract List<String> getTags();
}
