/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.inputs.random.generators;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FakeHttpMessageGenerator {

    private static final int MAX_WEIGHT = 50;

    private final String source;

    private final Random rand = new Random(System.currentTimeMillis());

    private final List<Weighted> GET_RESOURCES = new ArrayList<Weighted>() {{
        add(new Resource("/login", "LoginController", "login", 10));
        add(new Resource("/users", "UsersController", "index", 2));
        add(new Resource("/posts", "PostsController", "index", 40));
        add(new Resource("/posts/45326", "PostsController", "show", 12));
        add(new Resource("/posts/45326/edit", "PostsController", "edit", 1));
    }};

    private final List<Weighted> USER_IDS = new ArrayList<Weighted>() {{
        add(new UserId(9001, 10));
        add(new UserId(54351, 1));
        add(new UserId(74422, 5));
        add(new UserId(6476752, 12));
        add(new UserId(6469981, 40));
    }};

    public FakeHttpMessageGenerator(String source) {
        this.source = source;
    }

    public Message generate() {
        int x = rand.nextInt(100);
        int y = rand.nextInt(100);

        boolean isSuccessful = (y < 98);

        if (x <= 95) {
            // GET
            return buildGETMessage(isSuccessful);
        } else if(x > 95 && x <= 98) {
            // POST
            return buildPOSTMessage(isSuccessful);
        } else if(x == 99) {
            // DELETE
            return buildDELETEMessage(isSuccessful);
        } else {
            // PUT
            return buildPUTMessage(isSuccessful);
        }
    }

    private String shortMessage(String method, String resource, int code, int tookMs) {
        StringBuilder sb = new StringBuilder();

        sb.append(method).append(" ").append(resource)
                .append(" [").append(code).append("]")
                .append(" ").append(tookMs).append("ms");

        return sb.toString();
    }

    private <T extends Weighted> T getWeighted(List<Weighted> list) {
        while (true) {
            int x = rand.nextInt(MAX_WEIGHT);
            Weighted obj = list.get(rand.nextInt(list.size()));

            if (obj.getWeight() >= x) {
                return (T) obj;
            }
        }
    }

    // GET

    private Message buildGETMessage(boolean isSuccessful) {
        return get(isSuccessful);
    }

    private Message get(boolean isSuccessful) {
        Resource resource = getWeighted(GET_RESOURCES);

        int msBase = 100;
        int deviation = 30;
        int code = isSuccessful ? 200 : 500;

        if (!isSuccessful && rand.nextInt(5) == 1) {
            // Simulate an internal API timeout from time to time.
            msBase = 5000;
            deviation = 10;
            code = 504;
        } else if (rand.nextInt(500) == 1) {
            // ...or just something a bit too slow
            msBase = 400;
        }

        int tookMs = org.graylog2.inputs.random.generators.Tools.deviation(msBase, deviation, rand);
        UserId userId = getWeighted(USER_IDS);

        Message msg = new Message(shortMessage("GET", resource.getResource(), code, tookMs), source, new DateTime());

        msg.addField("http_method", "GET");
        msg.addField("http_response_code", code);

        msg.addField("resource", resource.getResource());
        msg.addField("controller", resource.getController());
        msg.addField("action", resource.getAction());

        msg.addField("user_id", userId.getId());
        msg.addField("took_ms", tookMs);

        return msg;
    }

    // POST

    private Message buildPOSTMessage(boolean isSuccessful) {
        return isSuccessful ? successfulPOST() : failedPOST();
    }

    private Message successfulPOST() {
        return new Message("successful POST", source, new DateTime());
    }

    private Message failedPOST() {
        return new Message("failed POST", source, new DateTime());
    }

    // PUT

    private Message buildPUTMessage(boolean isSuccessful) {
        return isSuccessful ? successfulPUT() : failedPUT();
    }

    private Message successfulPUT() {
        return new Message("successful PUT", source, new DateTime());
    }

    private Message failedPUT() {
        return new Message("failed PUT", source, new DateTime());
    }

    // DELETE

    private Message buildDELETEMessage(boolean isSuccessful) {
        return isSuccessful ? successfulDELETE() : failedDELETE();
    }

    private Message successfulDELETE() {
        return new Message("successful DELETE", source, new DateTime());
    }

    private Message failedDELETE() {
        return new Message("failed DELETE", source, new DateTime());
    }

    private abstract class Weighted {

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

    private class Resource extends Weighted {

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

    private class UserId extends Weighted {

        private final int id;

        public UserId(int id, int weight) {
            super(weight);

            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

}
