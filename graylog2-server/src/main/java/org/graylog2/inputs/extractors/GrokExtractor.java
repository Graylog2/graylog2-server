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
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;
import org.graylog2.ConfigurationException;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class GrokExtractor extends Extractor {
    private static final Logger log = LoggerFactory.getLogger(GrokExtractor.class);
    public static final String PATTERNS = "USERNAME [a-zA-Z0-9._-]+\n" +
            "USER %{USERNAME}\n" +
            "INT (?:[+-]?(?:[0-9]+))\n" +
            "BASE10NUM (?<![0-9.+-])(?>[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+)))\n" +
            "NUMBER (?:%{BASE10NUM})\n" +
            "BASE16NUM (?<![0-9A-Fa-f])(?:[+-]?(?:0x)?(?:[0-9A-Fa-f]+))\n" +
            "BASE16FLOAT \\b(?<![0-9A-Fa-f.])(?:[+-]?(?:0x)?(?:(?:[0-9A-Fa-f]+(?:\\.[0-9A-Fa-f]*)?)|(?:\\.[0-9A-Fa-f]+)))\\b\n" +
            "\n" +
            "POSINT \\b(?:[1-9][0-9]*)\\b\n" +
            "NONNEGINT \\b(?:[0-9]+)\\b\n" +
            "WORD \\b\\w+\\b\n" +
            "NOTSPACE \\S+\n" +
            "SPACE \\s*\n" +
            "DATA .*?\n" +
            "GREEDYDATA .*\n" +
            "QUOTEDSTRING (?>(?<!\\\\)(?>\"(?>\\\\.|[^\\\\\"]+)+\"|\"\"|(?>'(?>\\\\.|[^\\\\']+)+')|''|(?>`(?>\\\\.|[^\\\\`]+)+`)|``))\n" +
            "UUID [A-Fa-f0-9]{8}-(?:[A-Fa-f0-9]{4}-){3}[A-Fa-f0-9]{12}\n" +
            "\n" +
            "# Networking\n" +
            "MAC (?:%{CISCOMAC}|%{WINDOWSMAC}|%{COMMONMAC})\n" +
            "CISCOMAC (?:(?:[A-Fa-f0-9]{4}\\.){2}[A-Fa-f0-9]{4})\n" +
            "WINDOWSMAC (?:(?:[A-Fa-f0-9]{2}-){5}[A-Fa-f0-9]{2})\n" +
            "COMMONMAC (?:(?:[A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2})\n" +
            "IPV6 ((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\n" +
            "IPV4 (?<![0-9])(?:(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}))(?![0-9])\n" +
            "IP (?:%{IPV6}|%{IPV4})\n" +
            "HOSTNAME \\b(?:[0-9A-Za-z][0-9A-Za-z-]{0,62})(?:\\.(?:[0-9A-Za-z][0-9A-Za-z-]{0,62}))*(\\.?|\\b)\n" +
            "HOST %{HOSTNAME}\n" +
            "IPORHOST (?:%{HOSTNAME}|%{IP})\n" +
            "HOSTPORT %{IPORHOST}:%{POSINT}\n" +
            "\n" +
            "# paths\n" +
            "PATH (?:%{UNIXPATH}|%{WINPATH})\n" +
            "UNIXPATH (?>/(?>[\\w_%!$@:.,~-]+|\\\\.)*)+\n" +
            "TTY (?:/dev/(pts|tty([pq])?)(\\w+)?/?(?:[0-9]+))\n" +
            "WINPATH (?>[A-Za-z]+:|\\\\)(?:\\\\[^\\\\?*]*)+\n" +
            "URIPROTO [A-Za-z]+(\\+[A-Za-z+]+)?\n" +
            "URIHOST %{IPORHOST}(?::%{POSINT:port})?\n" +
            "# uripath comes loosely from RFC1738, but mostly from what Firefox\n" +
            "# doesn't turn into %XX\n" +
            "URIPATH (?:/[A-Za-z0-9$.+!*'(){},~:;=@#%_\\-]*)+\n" +
            "#URIPARAM \\?(?:[A-Za-z0-9]+(?:=(?:[^&]*))?(?:&(?:[A-Za-z0-9]+(?:=(?:[^&]*))?)?)*)?\n" +
            "URIPARAM \\?[A-Za-z0-9$.+!*'|(){},~@#%&/=:;_?\\-\\[\\]]*\n" +
            "URIPATHPARAM %{URIPATH}(?:%{URIPARAM})?\n" +
            "URI %{URIPROTO}://(?:%{USER}(?::[^@]*)?@)?(?:%{URIHOST})?(?:%{URIPATHPARAM})?\n" +
            "\n" +
            "# Months: January, Feb, 3, 03, 12, December\n" +
            "MONTH \\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\b\n" +
            "MONTHNUM (?:0?[1-9]|1[0-2])\n" +
            "MONTHNUM2 (?:0[1-9]|1[0-2])\n" +
            "MONTHDAY (?:(?:0[1-9])|(?:[12][0-9])|(?:3[01])|[1-9])\n" +
            "\n" +
            "# Days: Monday, Tue, Thu, etc...\n" +
            "DAY (?:Mon(?:day)?|Tue(?:sday)?|Wed(?:nesday)?|Thu(?:rsday)?|Fri(?:day)?|Sat(?:urday)?|Sun(?:day)?)\n" +
            "\n" +
            "# Years?\n" +
            "YEAR (?>\\d\\d){1,2}\n" +
            "HOUR (?:2[0123]|[01]?[0-9])\n" +
            "MINUTE (?:[0-5][0-9])\n" +
            "# '60' is a leap second in most time standards and thus is valid.\n" +
            "SECOND (?:(?:[0-5]?[0-9]|60)(?:[:.,][0-9]+)?)\n" +
            "TIME (?!<[0-9])%{HOUR}:%{MINUTE}(?::%{SECOND})(?![0-9])\n" +
            "# datestamp is YYYY/MM/DD-HH:MM:SS.UUUU (or something like it)\n" +
            "DATE_US %{MONTHNUM}[/-]%{MONTHDAY}[/-]%{YEAR}\n" +
            "DATE_EU %{MONTHDAY}[./-]%{MONTHNUM}[./-]%{YEAR}\n" +
            "ISO8601_TIMEZONE (?:Z|[+-]%{HOUR}(?::?%{MINUTE}))\n" +
            "ISO8601_SECOND (?:%{SECOND}|60)\n" +
            "TIMESTAMP_ISO8601 %{YEAR}-%{MONTHNUM}-%{MONTHDAY}[T ]%{HOUR}:?%{MINUTE}(?::?%{SECOND})?%{ISO8601_TIMEZONE}?\n" +
            "DATE %{DATE_US}|%{DATE_EU}\n" +
            "DATESTAMP %{DATE}[- ]%{TIME}\n" +
            "TZ (?:[PMCE][SD]T|UTC)\n" +
            "DATESTAMP_RFC822 %{DAY} %{MONTH} %{MONTHDAY} %{YEAR} %{TIME} %{TZ}\n" +
            "DATESTAMP_RFC2822 %{DAY}, %{MONTHDAY} %{MONTH} %{YEAR} %{TIME} %{ISO8601_TIMEZONE}\n" +
            "DATESTAMP_OTHER %{DAY} %{MONTH} %{MONTHDAY} %{TIME} %{TZ} %{YEAR}\n" +
            "DATESTAMP_EVENTLOG %{YEAR}%{MONTHNUM2}%{MONTHDAY}%{HOUR}%{MINUTE}%{SECOND}\n" +
            "\n" +
            "# Syslog Dates: Month Day HH:MM:SS\n" +
            "SYSLOGTIMESTAMP %{MONTH} +%{MONTHDAY} %{TIME}\n" +
            "PROG (?:[\\w._/%-]+)\n" +
            "SYSLOGPROG %{PROG:program}(?:\\[%{POSINT:pid}\\])?\n" +
            "SYSLOGHOST %{IPORHOST}\n" +
            "SYSLOGFACILITY <%{NONNEGINT:facility}.%{NONNEGINT:priority}>\n" +
            "HTTPDATE %{MONTHDAY}/%{MONTH}/%{YEAR}:%{TIME} %{INT}\n" +
            "\n" +
            "# Shortcuts\n" +
            "QS %{QUOTEDSTRING}\n" +
            "\n" +
            "# Log formats\n" +
            "SYSLOGBASE %{SYSLOGTIMESTAMP:timestamp} (?:%{SYSLOGFACILITY} )?%{SYSLOGHOST:logsource} %{SYSLOGPROG}:\n" +
            "COMMONAPACHELOG %{IPORHOST:clientip} %{USER:ident} %{USER:auth} \\[%{HTTPDATE:timestamp}\\] \"(?:%{WORD:verb} %{NOTSPACE:request}(?: HTTP/%{NUMBER:httpversion})?|%{DATA:rawrequest})\" %{NUMBER:response} (?:%{NUMBER:bytes}|-)\n" +
            "COMBINEDAPACHELOG %{COMMONAPACHELOG} %{QS:referrer} %{QS:agent}\n" +
            "\n" +
            "# Log Levels\n" +
            "LOGLEVEL ([Aa]lert|ALERT|[Tt]race|TRACE|[Dd]ebug|DEBUG|[Nn]otice|NOTICE|[Ii]nfo|INFO|[Ww]arn?(?:ing)?|WARN?(?:ING)?|[Ee]rr?(?:or)?|ERR?(?:OR)?|[Cc]rit?(?:ical)?|CRIT?(?:ICAL)?|[Ff]atal|FATAL|[Ss]evere|SEVERE|EMERG(?:ENCY)?|[Ee]merg(?:ency)?)";

    private final Grok grok = new Grok();

    public GrokExtractor(MetricRegistry metricRegistry,
                         String id,
                         String title,
                         int order,
                         CursorStrategy cursorStrategy,
                         String sourceField,
                         String targetField,
                         Map<String, Object> extractorConfig,
                         String creatorUserId,
                         List<Converter> converters,
                         ConditionType conditionType,
                         String conditionValue) throws ReservedFieldException, ConfigurationException {
        super(metricRegistry,
              id,
              title,
              order,
              Type.GROK,
              cursorStrategy,
              sourceField,
              targetField,
              extractorConfig,
              creatorUserId,
              converters,
              conditionType,
              conditionValue);
        if (extractorConfig == null || Strings.isNullOrEmpty((String) extractorConfig.get("grok_pattern"))) {
            throw new ConfigurationException("grok_pattern not set");
        }

        try {
            grok.addPatternFromReader(new StringReader(PATTERNS));
            grok.compile((String) extractorConfig.get("grok_pattern"));
        } catch (GrokException e) {
            log.error("Unable to parse grok patterns", e);
            throw new ConfigurationException("Unable to parse grok patterns");
        }
    }

    @Override
    protected Result[] run(String value) {

        // TODO did the patterns change, if so rebuild grok instance for this thread
        
        final Match match = grok.match(value);
        match.captures();
        final Map<String, Object> matches = match.toMap();
        final List<Result> results = Lists.newArrayListWithCapacity(matches.size());

        for (final Map.Entry<String, Object> entry : matches.entrySet()) {
            // never add null values to the results, those don't make sense for us
            if (entry.getValue() != null) {
                results.add(new Result(entry.getValue().toString(), entry.getKey(), -1, -1));
            }
        }

        return results.toArray(new Result[results.size()]);
    }
}
