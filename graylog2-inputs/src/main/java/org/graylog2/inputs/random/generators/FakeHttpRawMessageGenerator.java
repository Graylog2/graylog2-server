/**
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
package org.graylog2.inputs.random.generators;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.DELETE;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.GET;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.POST;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.PUT;

public class FakeHttpRawMessageGenerator {

    private static final int MAX_WEIGHT = 50;

    private final String source;

    private final Random rand = new Random(System.currentTimeMillis());

    private static final List<Resource> GET_RESOURCES = ImmutableList.of(
            new Resource("/login", "LoginController", "login", 10),
            new Resource("/users", "UsersController", "index", 2),
            new Resource("/posts", "PostsController", "index", 40),
            new Resource("/posts/45326", "PostsController", "show", 12),
            new Resource("/posts/45326/edit", "PostsController", "edit", 1));

    private static final ImmutableMap<String, Resource> RESOURCE_MAP = Maps.uniqueIndex(GET_RESOURCES, new Function<Resource, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Resource input) {
            if (input == null) {
                throw new IllegalStateException();
            }
            return input.getResource();
        }
    });

    private final List<UserId> USER_IDS = ImmutableList.of(
            new UserId(9001, 10),
            new UserId(54351, 1),
            new UserId(74422, 5),
            new UserId(6476752, 12),
            new UserId(6469981, 40));

    public FakeHttpRawMessageGenerator(String source) {
        this.source = source;
    }

    public static int rateDeviation(int val, int maxDeviation, Random rand) {
        int deviationPercent = rand.nextInt(maxDeviation);

        double x = val / 100.0 * deviationPercent;

        // Add or substract?
        double result = 0;
        if (rand.nextBoolean()) {
            result = val - x;
        } else {
            result = val + x;
        }

        if (result < 0) {
            return 1;
        } else {
            return Math.round((int) result);
        }
    }


    public GeneratorState generateState() {
        final GeneratorState generatorState = new GeneratorState();

        int methodProb = rand.nextInt(100);
        int successProb = rand.nextInt(100);

        generatorState.source = source;
        generatorState.isSuccessful = (successProb < 98);
        if (methodProb <= 95) {
            generatorState.method = GET;
            generatorState.isTimeout = rand.nextInt(5) == 1;
            generatorState.isSlowRequest = rand.nextInt(500) == 1;
            generatorState.userId = ((UserId) getWeighted(USER_IDS)).getId();
            generatorState.resource = ((Resource) getWeighted(GET_RESOURCES)).getResource();
        } else if (methodProb > 95 && methodProb <= 98) {
            generatorState.method = POST;
        } else if (methodProb == 99) {
            generatorState.method = DELETE;
        } else {
            generatorState.method = PUT;
        }

        return generatorState;
    }

    public static Message generateMessage(GeneratorState state) {
        final Random rand = new Random();
        final String source = state.source;
        final boolean isSuccessful = state.isSuccessful;

        Message msg = null;
        switch (state.method) {
            case GET:
                msg = get(state, rand);
                break;
            case POST:
                msg = isSuccessful ? successfulPOST(source) : failedPOST(source);
                break;
            case DELETE:
                msg = isSuccessful ? successfulDELETE(source) : failedDELETE(source);
                break;
            case PUT:
                msg = isSuccessful ? successfulPUT(source) : failedPUT(source);
                break;
        }
        return msg;
    }

    private static String shortMessage(String method, String resource, int code, int tookMs) {
        return method + " " + resource + " [" + code + "]" + " " + tookMs + "ms";
    }

    private <T extends Weighted> T getWeighted(List<? extends Weighted> list) {
        while (true) {
            int x = rand.nextInt(MAX_WEIGHT);
            Weighted obj = list.get(rand.nextInt(list.size()));

            if (obj.getWeight() >= x) {
                return (T) obj;
            }
        }
    }

    public static Message get(GeneratorState state,
                              Random rand) {
        final boolean isSuccessful = state.isSuccessful;

        Message msg;
        int msBase = 100;
        int deviation = 30;
        int code = isSuccessful ? 200 : 500;

        if (!isSuccessful && state.isTimeout) {
            // Simulate an internal API timeout from time to time.
            msBase = 5000;
            deviation = 10;
            code = 504;
        } else if (rand.nextInt(500) == 1) {
            // ...or just something a bit too slow
            msBase = 400;
        }

        int tookMs = rateDeviation(msBase, deviation, rand);

        msg = new Message(shortMessage("GET", state.resource, code, tookMs), state.source, Tools.iso8601());

        msg.addField("http_method", "GET");
        msg.addField("http_response_code", code);

        final Resource resource = RESOURCE_MAP.get(state.resource);
        msg.addField("resource", resource.getResource());
        msg.addField("controller", resource.getController());
        msg.addField("action", resource.getAction());

        msg.addField("user_id", state.userId);
        msg.addField("took_ms", tookMs);
        return msg;
    }

    private static Message successfulPOST(String source) {
        return new Message("successful POST", source, Tools.iso8601());
    }

    private static Message failedPOST(String source) {
        return new Message("failed POST", source, Tools.iso8601());
    }

    private static Message successfulPUT(String source) {
        return new Message("successful PUT", source, Tools.iso8601());
    }

    private static Message failedPUT(String source) {
        return new Message("failed PUT", source, Tools.iso8601());
    }

    private static Message successfulDELETE(String source) {
        return new Message("successful DELETE", source, Tools.iso8601());
    }

    private static Message failedDELETE(String source) {
        return new Message("failed DELETE", source, Tools.iso8601());
    }

    private static abstract class Weighted {

        protected final int weight;

        protected Weighted(int weight) {
            if (weight <= 0 || weight > MAX_WEIGHT) {
                throw new RuntimeException("Invalid resource weight: " + weight);
            }

            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }

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

    public static class GeneratorState {
        public String source;
        public boolean isSuccessful;
        public Method method;
        public boolean isTimeout;
        public boolean isSlowRequest;
        public int userId;
        public String resource;

        public enum Method {
            GET, POST, DELETE, PUT
        }
    }
}
