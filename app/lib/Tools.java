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
package lib;

import com.google.common.collect.Maps;
import play.Play;
import play.data.Form;
import play.mvc.Controller;

import java.util.Map;
import java.util.Random;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Tools {

    private Tools() { /* pure utility class */ }

    public static int random(int min, int max) {
        Random rand = new Random();
        return rand.nextInt(max - min + 1) + min;
    }

    public static Object removeTrailingNewline(Object x) {
        if (x == null) {
            return x;
        }

        if (x instanceof String) {
            String s = (String) x;
            if (s.endsWith("\n") || s.endsWith("\r")) {
                return s.substring(0, s.length()-1);
            } else {
                return x;
            }
        } else {
            return x;
        }
    }

    public static String syslogLevelToHuman(int level) {
        switch (level) {
            case 0:
                return "Emergency";
            case 1:
                return "Alert";
            case 2:
                return"Critical";
            case 3:
                return "Error";
            case 4:
                return "Warning";
            case 5:
                return "Notice";
            case 6:
                return "Info";
            case 7:
                return "Debug";
        }

        return "Invalid";
    }

    public static byte[] appSecretAsBytes(int keySize) {
        final String secret = Play.application().configuration().getString("application.secret");
        final StringBuilder sb = new StringBuilder(secret);
        while (sb.length() < keySize) {
            sb.append(secret);
        }
        // sb is now at least 16 bytes long, take the first keySize
        return sb.toString().substring(0, keySize).getBytes();
    }

    public static <T> Form<T> bindMultiValueFormFromRequest(Class<T> requestClass) {
        Map<String, String> newData = Maps.newHashMap();
        Map<String, String[]> urlFormEncoded = Controller.request().body().asFormUrlEncoded();
        if (urlFormEncoded != null) {
            for (String key : urlFormEncoded.keySet()) {
                String[] value = urlFormEncoded.get(key);
                if (value.length == 1) {
                    newData.put(key, value[0]);
                } else if (value.length > 1) {
                    for (int i = 0; i < value.length; i++) {
                        newData.put(key + "[" + i + "]", value[i]);
                    }
                }
            }
        }
        // bind to the MyEntity form object
        return new Form<>(requestClass).bind(newData);
    }
}
