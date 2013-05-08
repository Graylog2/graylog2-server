package models;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamRule {

    public static final Map<Integer, StreamRule> RULES = new HashMap<Integer, StreamRule>() {{
        put(1, new StreamRule(1, "match exactly"));
        put(2, new StreamRule(2, "match regular expression"));
        put(3, new StreamRule(3, "greater than", "be greater than"));
        put(4, new StreamRule(4, "smaller than", "be smaller than"));
    }};

    private final int id;
    private final String name;
    private final String sentenceRepresentation;

    public StreamRule(int id, String name) {
        this.id = id;
        this.name = name;
        this.sentenceRepresentation = name;
    }

    public StreamRule(int id, String name, String sentenceRepresentation) {
        this.id = id;
        this.name = name;
        this.sentenceRepresentation = sentenceRepresentation;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSentenceRepresentation() {
        return sentenceRepresentation;
    }

}
