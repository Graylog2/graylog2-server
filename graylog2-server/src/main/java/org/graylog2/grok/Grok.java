package org.graylog2.grok;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.joni.exception.ValueException;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class Grok {

    private static final Regex GROK_REGEX = new Regex(
            "%\\{(?<name>(?<pattern>[A-z0-9]+)(?::(?<subname>[A-z0-9_:;\\-\\/\\s\\.']+))?)(?:=(?<definition>(?:(?:[^{}]+|\\.+)+)+))?\\}");

    private final Regex regex;
    private final Map<String, String> grokPatterns;
    private final boolean namedCapturesOnly;

    public Grok(Map<String, String> grokPatterns, String grokString, boolean namedCapturesOnly) {
        // TODO: this is rather pointless: defensive copy as we can add definitions as well
        this.grokPatterns = Maps.newHashMap(grokPatterns);
        this.namedCapturesOnly = namedCapturesOnly;

        byte[] joniRegexBytes = convert(grokString).getBytes(StandardCharsets.UTF_8);
        regex = new Regex(joniRegexBytes, 0, joniRegexBytes.length, Option.DEFAULT, UTF8Encoding.INSTANCE);
    }

    private String convert(String grokString) {
        byte[] grokStringBytes = grokString.getBytes(StandardCharsets.UTF_8);
        Matcher matcher = GROK_REGEX.matcher(grokStringBytes);

        int matchedLocation = matcher.search(0, grokStringBytes.length, Option.NONE);
        if (matchedLocation != -1) {
            final Region region = matcher.getEagerRegion();

            //noinspection OptionalGetWithoutIsPresent
            final String name = namedGroup("name", region, grokStringBytes).get();
            // will be absent or "", we only need to know whether it exists, not the actual content here, we get that via the name group
            final Optional<String> subName = namedGroup("subname", region, null);
            final Optional<String> definition = namedGroup("definition", region, grokStringBytes);
            final Optional<String> pattern = namedGroup("pattern", region, grokStringBytes);

            if (!pattern.isPresent()) {
                throw new IllegalStateException("Could not expand grok pattern, malformed pattern: " + grokString);
            }

            final String patternName = pattern.get();
            if (definition.isPresent() && !Strings.isNullOrEmpty(definition.get())) {
                grokPatterns.put(patternName, definition.get());
            }
            String referencedPattern = grokPatterns.get(patternName);

            String expansion;
            if (subName.isPresent()) {
                expansion = String.format(Locale.US, "(?<%s>%s)", name, referencedPattern);
            } else {
                expansion = String.format(Locale.US,
                                          "(?<%s>%s)",
                                          patternName + "_" + String.valueOf(matchedLocation),
                                          referencedPattern);
            }
            String verbatim = new String(grokStringBytes, 0, matchedLocation, StandardCharsets.UTF_8);
            String rest = new String(grokStringBytes,
                                     region.end[0],
                                     grokStringBytes.length - region.end[0],
                                     StandardCharsets.UTF_8);
            return verbatim + convert(expansion + rest);
        }
        // nothing left to replace
        return grokString;
    }

    private Optional<String> namedGroup(String name, Region region, @Nullable byte[] patternBytes) {
        try {
            final byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            int number = GROK_REGEX.nameToBackrefNumber(nameBytes, 0, nameBytes.length, region);

            int begin = region.beg[number];
            int end = region.end[number];
            if (patternBytes != null) {
                return Optional.of(new String(patternBytes, begin, end - begin, StandardCharsets.UTF_8));
            } else {
                // cheaper signal that a named group was used
                return Optional.of("");
            }
        } catch (StringIndexOutOfBoundsException e) {
            return Optional.empty();
        } catch (ValueException e) {
            return Optional.empty();
        }
    }

    public GrokMatch match(String input) {
        return new GrokMatch(regex, input, namedCapturesOnly);
    }

}
