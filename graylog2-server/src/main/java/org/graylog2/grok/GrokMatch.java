package org.graylog2.grok;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.MoreObjects.firstNonNull;

public class GrokMatch {

    private static final DateFormat DATE_TIME_INSTANCE =
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH);
    private static final Splitter SPLITTER = Splitter.on(Pattern.compile(":|;")).limit(4).trimResults();

    private final Regex regex;
    private final boolean namedCapturesOnly;
    private int location;
    private final byte[] inputBytes;
    private final Region region;

    public GrokMatch(Regex regex, String input, boolean namedCapturesOnly) {

        this.regex = regex;
        this.namedCapturesOnly = namedCapturesOnly;
        input = Strings.nullToEmpty(input);

        inputBytes = input.getBytes(StandardCharsets.UTF_8);
        final Matcher matcher = regex.matcher(inputBytes);
        location = matcher.search(0, inputBytes.length, Option.DEFAULT);
        region = matcher.getEagerRegion();
    }


    public boolean matches() {
        return location != -1 && regex.numberOfNames() > 0;
    }

    public Map<String, Object> captures() {
        Map<String, Object> fields = new HashMap<>();

        if (matches()) {
            regex.namedBackrefIterator().forEachRemaining(nameEntry -> {
                int number = nameEntry.getBackRefs()[0];

                String groupName = new String(nameEntry.name,
                                              nameEntry.nameP,
                                              nameEntry.nameEnd - nameEntry.nameP,
                                              StandardCharsets.UTF_8);
                String capture = null;
                if (region.beg[number] >= 0) {
                    capture = new String(inputBytes, region.beg[number], region.end[number] - region.beg[number],
                                         StandardCharsets.UTF_8);
                }

                // groupname is: PATTERN:captureName[:type[:options]]
                // colons can also be semicolons
                final Iterator<String> elements = SPLITTER.split(groupName).iterator();
                final String pattern = elements.next();
                final String captureName = Iterators.getNext(elements, null);
                final String type = Iterators.getNext(elements, "string");
                final String option = Iterators.getNext(elements, null);

                if ((namedCapturesOnly && (captureName == null)) || "UNWANTED".equals(captureName)) {
                    return;
                }
                fields.put(firstNonNull(captureName, pattern),
                           convertValue(capture, type, option));
            });
        }
        return fields;
    }

    private Object convertValue(String value, String type, String option) {
        switch (type) {
            case "byte":
                return Byte.parseByte(value);
            case "boolean":
                return Boolean.parseBoolean(value);
            case "short":
                return Short.parseShort(value);
            case "int":
                return Integer.parseInt(value);
            case "long":
                return Long.parseLong(value);
            case "float":
                return Float.parseFloat(value);
            case "double":
                return Double.parseDouble(value);
            case "date":
            case "datetime":
                try {
                    if (option == null) {
                        return DATE_TIME_INSTANCE.parse(value);
                    } else {
                        SimpleDateFormat formatter = new SimpleDateFormat(option, Locale.ENGLISH);
                        return formatter.parse(value);
                    }
                } catch (ParseException ignored) {
                    return null;
                }

            case "string":
                break;
            default:
                return value;
        }
        return value;
    }

}
