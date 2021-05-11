package org.graylog.plugins.views.search.export;

import org.bson.types.ObjectId;

import java.util.function.Supplier;

public class ExportJobFactory {
    private final Supplier<String> idGenerator = () -> new ObjectId().toHexString();

    public ExportJob fromMessagesRequest(MessagesRequest messagesRequest) {
        return MessagesRequestExportJob.fromMessagesRequest(idGenerator.get(), messagesRequest);
    }

    public ExportJob forSearch(String searchId, ResultFormat resultFormat) {
        return SearchExportJob.forSearch(idGenerator.get(), searchId, resultFormat);
    }

    public ExportJob forSearchType(String searchId, String searchTypeId, ResultFormat resultFormat) {
        return SearchTypeExportJob.forSearchType(idGenerator.get(), searchId, searchTypeId, resultFormat);
    }
}
