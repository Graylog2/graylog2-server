/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.alarms.transports;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import org.graylog2.plugin.alarms.Alarm;

public class HtmlEmailLayout implements EmailLayout
{
    private static final String CONTENT_TYPE = "text/html";
    private static final String LINK_TEXT = "View Messages in Graylog2";
    private static final String SEPARATOR = "<hr style=\"height:1px;border:0px;color:#828181;background-color:#828181;\"/>\n";
    
    // Work around thread safety issues, while still reusing instances
    private static ThreadLocal<DateFormat> TIMESTAMP_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };
    
    private String subjectPrefix;
    private String webURL;
    
    public void initialize(Map<String, String> pluginConfiguration) {
        this.subjectPrefix = pluginConfiguration.get("subject_prefix");
        this.webURL = pluginConfiguration.get("web_interface_url");
    }
    
    public String getContentType() {
        return CONTENT_TYPE;
    }
    
    public String getSubject(Alarm alarm, long unixTimestamp) {
        String subject = alarm.getTopic();
        
        if (subjectPrefix != null && !subjectPrefix.isEmpty()) {
            subject = subjectPrefix + " " + subject;
        }
        
        return subject;
    }
    
    public String formatMessageBody(Alarm alarm, long unixTimestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n").append("<body>\n");
        
        sb.append(HtmlUtil.encode(alarm.getDescription())).append("<br/>\n");
        sb.append(SEPARATOR);
        
        String messageURL = getAlarmURL(alarm, unixTimestamp);
        if (null != messageURL) {
            sb.append("<a href=\"").append(messageURL).append("\">").append(LINK_TEXT).append("</a>\n");
        }
        
        sb.append("</body>").append("</html>\n");
        
        return sb.toString();
    }
    
    private String getAlarmURL(Alarm alarm, long unixTimestamp) {
        return (null == webURL || webURL.isEmpty())
                ? null
                : webURL + "/streams/"
                        + alarm.getStream().getId().toString()
                        + "/messages?filters[date]=from+"
                        + urlEncode(getFromTimestamp(alarm, unixTimestamp))
                        + "+to+"
                        + urlEncode(getToTimestamp(alarm, unixTimestamp));
    }
    
    private String urlEncode(String string) {
        try {
            string = URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {} // Can't happen per Java spec - UTF-8 support is required
        return string;
    }
    
    private String getFromTimestamp(Alarm alarm, long unixTimestamp) {
        long fromTimestamp = unixTimestamp - (alarm.getStream().getAlarmTimespan() * 60);
        return formatTimestamp(fromTimestamp);
    }

    private String getToTimestamp(Alarm alarm, long unixTimestamp) {
        return formatTimestamp(unixTimestamp);
    }

    private String formatTimestamp(long unixTimestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(unixTimestamp * 1000);
        return TIMESTAMP_FORMAT.get().format(cal.getTime());
    }
}
