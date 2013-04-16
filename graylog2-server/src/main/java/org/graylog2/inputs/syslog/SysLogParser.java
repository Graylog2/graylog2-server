/**
 * Copyright 2013 Kay Roepke <kroepke@googlemail.com>
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
package org.graylog2.inputs.syslog;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 *
 */
public class SyslogParser {
    private final String message;

    private final SyslogMessage.Builder builder;

    // the current position in the input stream over the message
    private int position = 0;

    public SyslogParser(String msg) {
        message = msg;
        builder = SyslogMessage.builder();
    }

    public SyslogMessage parse() {
        System.err.println("trying to parse: " + message);
        if (la(1) == '<') {
            consume();
            // looks like syslog, try to parse the header
            header();
        } else {
            // TODO: try something else entirely. might be some cisco or completely unrelated message format
        }
        return builder.build();
    }

    /**
     * this tries to parse the syslog header value as described in either:
     * <ul>
     *  <li>http://tools.ietf.org/html/rfc3164#section-4.1</li>
     * <li>http://tools.ietf.org/html/rfc5424#section-6.2</li>
     * </ul>
     *
     * Note that the first document calls a different part "HEADER", namely the one following the prival.
     */
    private void header() {
        int start = position;
        if (isDigit(la(1))) {
            consume();
            if (isDigit(la(1))) {
                consume();
                if (isDigit(la(1))) {
                    consume();
                }
            }
            // format is described at http://tools.ietf.org/html/rfc5424#section-6.2.1
            String priVal = getText(start);
            int priValNum = Integer.parseInt(priVal);
            int facility = priValNum >> 3;
            builder.facility(facility);
            builder.severity(priValNum - (facility << 3));
            
            // check that the next character is in fact the closing '>'
            if ('>' == la(1)) {
                consume();
            } else {
                // TODO should somehow signal an error. use exception? potentially expensive
            }
        }
        
        // input position is now after the facility/severity value or we encountered an error (yet unhandled in this code)
        // RFC 5424 mandates the version number at this point. Only one version has ever been assigned.
        if ('1' == la(1) && ' ' == la(2)) {
            // this is structured syslog a la RFC 5424
            consume(); // the version
            consume(); // the space
            timestamp(true);
            hostnameOrIP();
        } else {
            // this is unstructured syslog a la RFC 3164 (or something like it)
            timestamp(false);
            hostnameOrIP();
        }
    }

    /**
     * Parse a syslog-like timestamp.
     * It should be made to handle all kinds of common timestamps.
     *
     * @param guessRfc3339Timestamp if true, initially try to parse a simplified ISO8601 timestamp, fall back if necessary
     */
    private void timestamp(boolean guessRfc3339Timestamp) {
        boolean parseRfc3339Successful = false;
        if (guessRfc3339Timestamp) {
            if ('-' == la(1)) {
                // NILVALUE, means we don't have a supplied timestamp TODO new Date() instead?
                consume();
            } else {
                // we just check for common signature of dates, meaning the fixed positions of the separators:
                // YYYY-MM-DDTHH:MM:SS.
                // 12345678901234567890
                int dateStart = position;
                if ('-' == la(5) && '-' == la(8) &&
                        'T' == la(11) && ':' == la(14) && ':' == la(17) && isDigit(la(18)) && isDigit(la(19))) {
                    // all the separators were there, let's try to figure out if the variable part also matches
                    // and try to find the end of the date string
                    consume(19);

                    // optional fractional seconds start with '.' and run up to the first non-digit character
                    if ('.' == la(1)) {
                        consume();
                        while ('0' <= la(1) && la(1) <= '9') {
                            consume();
                        }
                    }
                    // next is the timezone, either 'Z' for UTC or a TZ offset
                    if ('Z' == la(1)) {
                        consume();
                    } else if ('-' == la(1) || '+' == la(1)) {
                        consume();
                        // timezone offset is "HH:MM"
                        // again just check the structure
                        if (':' == la(3)) {
                            consume(5);
                        }
                    }
                    String dateString = getText(dateStart);
                    DateTime dateTime = new DateTime(dateString);
                    parseRfc3339Successful = true;
                    builder.timestamp(dateTime);
                    if (' ' == la(1)) {
                        // field should have a space separator trailing
                        consume();
                    } else {
                        // TODO complain
                    }
                }
            }
        }
        if (!parseRfc3339Successful) {
            // try the legacy date format of http://tools.ietf.org/html/rfc3164#section-4.1.2
            // Mmm dd hh:mm:ss
            int month = 0;
            // be very liberal in what is accepted here, just for speed
            if ('A' == la(1)) {
                if ('p' == la(2)) { month = 4; }
                if ('u' == la(2)) { month = 8; }
            }
            if ('D' == la(1)) { month = 12; }
            if ('F' == la(1)) { month = 2; }
            if ('J' == la(1)) {
                if ('a' == la(2)) { month = 1; }
                if ('u' == la(2)) {
                    if ('l' == la(3)) { month = 7; }
                    if ('n' == la(3)) { month = 6; }
                }
                if ('M' == la(1)) {
                    if ('a' == la(2)) {
                        if ('r' == la(3)) { month = 3; }
                        if ('y' == la(3)) { month = 5; }
                    }
                }
            }
            if ('N' == la(1)) { month = 11; }
            if ('O' == la(1)) { month = 10; }
            if ('S' == la(1)) { month = 9; }
            consume(3);

            if (month == 0) {
                // TODO complain
            }

            if (' ' == la(1)) {
                consume();
            } else {
                // TODO complain
            }

            int day = 0;
            // one (http://jira.graylog2.org/browse/SERVER-287) or two digits day
            if (' ' == la(1) && isDigit(la(2))) {
                // syslog as per rfc3164 (skip extra space)
                consume();
            }
            if (isDigit(la(1))) {
                if (isDigit(la(2))) {
                    // either consumed leading space before, or single character day, SERVER-287 case
                    day = intDigit(la(1)) * 10 + intDigit(la(2));
                } else if (' ' == la(2)) {
                    day = intDigit(la(1));
                }
                consume(2);
            }

            // now consume the trailing space (wasn't consumed above!)
            if (' ' == la(1)) {
                consume();
            } else {
                // TODO complain
            }

            // the time part, should always look like HH:MM:SS
            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            if (isDigit(la(1)) && isDigit(la(2)) && ':' == la(3)) {
                hours += intDigit(la(1)) * 10 + intDigit(la(2));
                consume(3);
            }
            if (isDigit(la(1)) && isDigit(la(2)) && ':' == la(3)) {
                minutes += intDigit(la(1)) * 10 + intDigit(la(2));
                consume(3);
            }
            if (isDigit(la(1)) && isDigit(la(2)) && ' ' == la(3)) {
                seconds += intDigit(la(1)) * 10 + intDigit(la(2));
                consume(3);
            }
            // this is so sad.
            int year = new DateTime().getYear();
            builder.timestamp(new DateTime(year, month, day, hours, minutes, seconds, 0, DateTimeZone.UTC));
        }
    }

    private void hostnameOrIP() {
        int hostnameStart = position;
        if ('-' == la(1) && ' ' == la(2)) {
            // NILVALUE. no host is given.
            builder.hostname(null);
            consume(2);
        } else {
            // hostname or IP, right now we don't care which format is used.
            while (' ' != la(1)) {
                consume();
            }
            builder.hostname(getText(hostnameStart));
        }


    }

    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    private static int intDigit(final char c) {
        return c - '0';
    }

    // extremely simplistic input "stream" with look-ahead

    // return the character at the look-ahead offset in the message
    private char la(int offset) {
        int index = position + offset - 1;
        if (index >= message.length()) {
            return Character.MIN_VALUE;
        }
        return message.charAt(index);
    }

    // move the current input point ahead in the message
    private void consume() {
        consume(1);
    }

    private void consume(int i) {
        position += i;
    }

    // returns the text in message from startMaker until the current position, inclusive
    private String getText(int startMarker) {
        return message.substring(startMarker, position);
    }


    public static void main(String[] args) {
        String msg = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry";

        SyslogMessage parsedMessage = new SyslogParser(msg).parse();
        msg = "<86>Dec 24 17:05:01 foo-bar CRON[10049]: pam_unix(cron:session): session closed for user root";
        SyslogMessage parsedMessage2 = new SyslogParser(msg).parse();
        msg = "<38>Feb 5 10:18:12 foo-bar sshd[593115]: Accepted publickey for root from 94.XXX.XXX.XXX port 5992 ssh2";
        SyslogMessage parsedMessage3 = new SyslogParser(msg).parse();

        System.err.println(parsedMessage.getDateTime().withZone(DateTimeZone.UTC).toString());
        System.err.println(parsedMessage2.getDateTime().withZone(DateTimeZone.UTC).toString());
        System.err.println(parsedMessage3.getDateTime().withZone(DateTimeZone.UTC).toString());
    }
}
