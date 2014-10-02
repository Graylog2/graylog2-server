/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.lib;

import com.google.common.collect.Maps;
import play.api.templates.Html;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Http;
import scala.collection.mutable.StringBuilder;

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

    /**
     * Turn the argument into either itself or a HTML-safe non-breaking space if it is a string.
     * @return HTML-safe non-breaking space of the argument itself
     */
    public static Object orNbsp(Object x) {
        if (x == null) {
            return new Html(new StringBuilder("&nbsp;"));
        }

        if (x instanceof String) {
            final String s = x.toString();
            if (s.isEmpty()) {
                return new Html(new StringBuilder("&nbsp;"));
            }
        }
        return x;
    }

    public static Object optionalLongValue(Object value) {
        if (value instanceof Double || value instanceof Float) {
            Double doubleValue = (Double) value;
            if (doubleValue == doubleValue.longValue()) {
                // it's actually representable as a Long, use that instead
                return doubleValue.longValue();
            }
        }
        // it's either something else or not representable as a Long, just use whatever it is
        return value;
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

    public static String stringSearchParamOrEmpty(Http.Request request, String param) {
        if (request.getQueryString(param) == null || request.getQueryString(param).isEmpty()) {
            return "";
        } else {
            return request.getQueryString(param);
        }
    }

    public static int intSearchParamOrEmpty(Http.Request request, String param) {
        if (request.getQueryString(param) == null || request.getQueryString(param).isEmpty()) {
            return 0;
        } else {
            try {
                return Integer.parseInt(request.getQueryString(param));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    public static String byteToHuman(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(1024));

        String pre = "kMGTPE".charAt(exp-1) + "i";
        return String.format("%.1f%sB", bytes / Math.pow(1024, exp), pre);
    }

    public static <T> T firstNonNull(T defaultValue, T... objects) {
        for (T object : objects) {
            if (object != null) {
                return object;
            }
        }
        return defaultValue;
    }

    public static boolean apiRequestShouldExtendSession() {
        try {
            return !("true".equalsIgnoreCase(Http.Context.current().request().getHeader("X-Graylog2-No-Session-Extension")));
        } catch (Exception e) {
            return true;
        }
    }

    public static Throwable rootCause(Throwable t) {
        Throwable rootCause = t;
        Throwable cause = rootCause.getCause();
        while (cause != null && cause != rootCause) {
            rootCause = cause;
            cause = cause.getCause();
        }
        return rootCause;
    }
}
