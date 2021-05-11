package org.graylog.plugins.views.search.export;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.joda.time.DateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = ExportJob.FIELD_TYPE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MessagesRequestExportJob.class, name = MessagesRequestExportJob.TYPE),
        @JsonSubTypes.Type(value = SearchExportJob.class, name = SearchExportJob.TYPE),
        @JsonSubTypes.Type(value = SearchTypeExportJob.class, name = SearchTypeExportJob.TYPE)
})
public interface ExportJob {
    String FIELD_TYPE = "type";
    String FIELD_ID = "_id";
    String FIELD_CREATED_AT = "created_at";
    String id();
    DateTime createdAt();
}
