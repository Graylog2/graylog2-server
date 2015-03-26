package models.sockjs;

import java.util.List;

public class SubscribeMetricsUpdates extends SockJsCommand {
    public String nodeId;
    public List<String> metrics;
}
