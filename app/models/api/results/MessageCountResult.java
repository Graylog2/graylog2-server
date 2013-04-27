package models.api.results;

public class MessageCountResult {

    private final int eventsCount;

    public MessageCountResult(int eventsCount) {
        this.eventsCount = eventsCount;
    }

    public int getEventsCount() {
        return eventsCount;
    }

}
