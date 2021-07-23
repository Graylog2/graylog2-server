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
package org.graylog2.inputs.random.generators;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Random;

import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.DELETE;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.GET;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.POST;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.PUT;

public class FakeHttpRawMessageGenerator extends FakeMessageGenerator {
    private static final ImmutableList<Resource> GET_RESOURCES = ImmutableList.of(
            new Resource("/login", "LoginController", "login", 10),
            new Resource("/users", "UsersController", "index", 2),
            new Resource("/posts", "PostsController", "index", 40),
            new Resource("/posts/45326", "PostsController", "show", 12),
            new Resource("/posts/45326/edit", "PostsController", "edit", 1));

    private static final ImmutableMap<String, Resource> RESOURCE_MAP = Maps.uniqueIndex(GET_RESOURCES, Resource::getResource);

    private static final ImmutableList<UserId> USER_IDS = ImmutableList.of(
            new UserId(9001, 10),
            new UserId(54351, 1),
            new UserId(74422, 5),
            new UserId(6476752, 12),
            new UserId(6469981, 40));

    public FakeHttpRawMessageGenerator(Configuration configuration) {
        super(configuration);
    }

    @Override
    public GeneratorState generateState() {
        final GeneratorState generatorState = new GeneratorState();

        final int methodProb = RANDOM.nextInt(100);
        final int successProb = RANDOM.nextInt(100);

        generatorState.msgSequenceNr = this.msgSequenceNumber++;
        generatorState.source = this.source;
        generatorState.isSuccessful = successProb < 98;
        generatorState.isTimeout = RANDOM.nextInt(5) == 1;
        generatorState.isSlowRequest = RANDOM.nextInt(500) == 1;
        generatorState.userId = ((UserId) getWeighted(USER_IDS)).getId();
        generatorState.resource = ((Resource) getWeighted(GET_RESOURCES)).getResource();
        generatorState.timeStamp = Tools.nowUTC();

        if (methodProb <= 85) {
            generatorState.method = GET;
        } else if (methodProb > 85 && methodProb <= 90) {
            generatorState.method = POST;
        } else if (methodProb > 90 && methodProb <= 95) {
            generatorState.method = DELETE;
        } else {
            generatorState.method = PUT;
        }

        return generatorState;
    }

    public static Message generateMessage(GeneratorState state) {
        Message msg = null;
        switch (state.method) {
            case GET:
                msg = simulateGET(state, RANDOM);
                break;
            case POST:
                msg = simulatePOST(state, RANDOM);
                break;
            case DELETE:
                msg = simulateDELETE(state, RANDOM);
                break;
            case PUT:
                msg = simulatePUT(state, RANDOM);
                break;
        }
        return msg;
    }

    private static String shortMessage(DateTime ingestTime, GeneratorState.Method method, String resource, int code, int tookMs) {
        return ingestTime + " " + method + " " + resource + " [" + code + "]" + " " + tookMs + "ms";
    }

    private static Map<String, Object> ingestTimeFields(DateTime ingestTime) {
        return ImmutableMap.<String, Object>builder()
                .put("ingest_time", ingestTime.toString())
                .put("ingest_time_epoch", ingestTime.getMillis())
                .put("ingest_time_second", ingestTime.getSecondOfMinute())
                .put("ingest_time_minute", ingestTime.getMinuteOfHour())
                .put("ingest_time_hour", ingestTime.getHourOfDay())
                .put("ingest_time_day", ingestTime.getDayOfMonth())
                .put("ingest_time_month", ingestTime.getMonthOfYear())
                .put("ingest_time_year", ingestTime.getYear())
                .build();
    }

    private static Map<String, Object> resourceFields(Resource resource) {
        return ImmutableMap.<String, Object>builder()
                .put("resource", resource.getResource())
                .put("controller", resource.getController())
                .put("action", resource.getAction())
                .build();
    }

    private static Message createMessage(GeneratorState state, int httpCode, Resource resource, int tookMs) {
        final Message msg = new Message(shortMessage(state.timeStamp, state.method, state.resource, httpCode, tookMs), state.source, state.timeStamp);
        msg.addField("sequence_nr", state.msgSequenceNr);
        msg.addFields(ingestTimeFields(state.timeStamp));
        msg.addFields(resourceFields(resource));
        msg.addField("ticks", System.nanoTime());
        msg.addField("http_method", state.method.name());
        msg.addField("http_response_code", httpCode);
        msg.addField("user_id", state.userId);
        msg.addField("took_ms", tookMs);

        return msg;
    }

    public static Message simulateGET(GeneratorState state, Random rand) {
        int msBase = 50;
        int deviation = 30;
        int code = state.isSuccessful ? 200 : 500;
        if (!state.isSuccessful && state.isTimeout) {
            // Simulate an internal API timeout from time to time.
            msBase = 5000;
            deviation = 10;
            code = 504;
        } else if (rand.nextInt(500) == 1) {
            // ...or just something a bit too slow
            msBase = 400;
        }

        final Resource resource = RESOURCE_MAP.get(state.resource);
        final int tookMs = rateDeviation(msBase, deviation, rand);

        return createMessage(state, code, resource, tookMs);
    }


    private static Message simulatePOST(GeneratorState state, Random rand) {
        int msBase = 150;
        int deviation = 20;
        int code = state.isSuccessful ? 201 : 500;
        if (!state.isSuccessful && state.isTimeout) {
            // Simulate an internal API timeout from time to time.
            msBase = 5000;
            deviation = 18;
            code = 504;
        } else if (rand.nextInt(500) == 1) {
            // ...or just something a bit too slow
            msBase = 400;
        }

        final Resource resource = RESOURCE_MAP.get(state.resource);
        final int tookMs = rateDeviation(msBase, deviation, rand);

        return createMessage(state, code, resource, tookMs);
    }

    private static Message simulatePUT(GeneratorState state, Random rand) {
        int msBase = 100;
        int deviation = 30;
        int code = state.isSuccessful ? 200 : 500;
        if (!state.isSuccessful && state.isTimeout) {
            // Simulate an internal API timeout from time to time.
            msBase = 5000;
            deviation = 18;
            code = 504;
        } else if (rand.nextInt(500) == 1) {
            // ...or just something a bit too slow
            msBase = 400;
        }

        final Resource resource = RESOURCE_MAP.get(state.resource);
        final int tookMs = rateDeviation(msBase, deviation, rand);

        return createMessage(state, code, resource, tookMs);
    }

    private static Message simulateDELETE(GeneratorState state, Random rand) {
        int msBase = 75;
        int deviation = 40;
        int code = state.isSuccessful ? 204 : 500;
        if (!state.isSuccessful && state.isTimeout) {
            // Simulate an internal API timeout from time to time.
            msBase = 5000;
            deviation = 18;
            code = 504;
        } else if (rand.nextInt(500) == 1) {
            // ...or just something a bit too slow
            msBase = 400;
        }

        final Resource resource = RESOURCE_MAP.get(state.resource);
        final int tookMs = rateDeviation(msBase, deviation, rand);

        return createMessage(state, code, resource, tookMs);
    }

    private static class Resource extends Weighted {

        private final String resource;
        private final String controller;
        private final String action;

        public Resource(String resource, String controller, String action, int weight) {
            super(weight);

            this.resource = resource;
            this.controller = controller;
            this.action = action;
        }

        public String getResource() {
            return resource;
        }

        public String getController() {
            return controller;
        }

        public String getAction() {
            return action;
        }
    }

    private static class UserId extends Weighted {

        private final int id;

        public UserId(int id, int weight) {
            super(weight);

            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public static class GeneratorState extends FakeMessageGenerator.GeneratorState {
        public long msgSequenceNr;
        public String source;
        public boolean isSuccessful;
        public Method method;
        public boolean isTimeout;
        public boolean isSlowRequest;
        public int userId;
        public String resource;
        public DateTime timeStamp;

        public enum Method {
            GET, POST, DELETE, PUT
        }
    }
}
