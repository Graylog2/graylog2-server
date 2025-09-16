package org.graylog.aws.sqs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.Nullable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SQSMessage {

        @JsonProperty("Records")
        @Nullable
        public List<JsonNode> records;

        @JsonProperty("detail")
        @Nullable
        public JsonNode detail;

}


