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

import java.util.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FakeHttpMessageGenerator {

    private final String source;

    private final Random rand = new Random();

    public FakeHttpMessageGenerator(String source) {
        this.source = source;
    }

    public Message generate() {
        int x = rand.nextInt(100);
        int y = rand.nextInt(100);

        boolean isSuccessful = (y < 98);

        if (x <= 90) {
            // GET
            return buildGETMessage(isSuccessful);
        } else if(x > 90 && x <= 95) {
            // POST
            return buildPOSTMessage(isSuccessful);
        } else if(x > 95 && x <= 97) {
            // DELETE
            return buildDELETEMessage(isSuccessful);
        } else {
            // PUT
            return buildPUTMessage(isSuccessful);
        }
    }

    // GET

    public Message buildGETMessage(boolean isSuccessful) {
        return isSuccessful ? successfulGET() : failedGET();
    }

    public Message successfulGET() {
        return new Message("successful GET", source, Tools.getUTCTimestampWithMilliseconds());
    }

    public Message failedGET() {
        return new Message("failed GET", source, Tools.getUTCTimestampWithMilliseconds());
    }

    // POST

    public Message buildPOSTMessage(boolean isSuccessful) {
        return isSuccessful ? successfulPOST() : failedPOST();
    }

    public Message successfulPOST() {
        return new Message("successful POST", source, Tools.getUTCTimestampWithMilliseconds());
    }

    public Message failedPOST() {
        return new Message("failed POST", source, Tools.getUTCTimestampWithMilliseconds());
    }

    // PUT

    public Message buildPUTMessage(boolean isSuccessful) {
        return isSuccessful ? successfulPUT() : failedPUT();
    }

    public Message successfulPUT() {
        return new Message("successful PUT", source, Tools.getUTCTimestampWithMilliseconds());
    }

    public Message failedPUT() {
        return new Message("failed PUT", source, Tools.getUTCTimestampWithMilliseconds());
    }

    // DELETE

    public Message buildDELETEMessage(boolean isSuccessful) {
        return isSuccessful ? successfulDELETE() : failedDELETE();
    }

    public Message successfulDELETE() {
        return new Message("successful DELETE", source, Tools.getUTCTimestampWithMilliseconds());
    }

    public Message failedDELETE() {
        return new Message("failed DELETE", source, Tools.getUTCTimestampWithMilliseconds());
    }

}
