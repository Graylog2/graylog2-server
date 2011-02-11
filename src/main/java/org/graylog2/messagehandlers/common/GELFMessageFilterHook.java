/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graylog2.Main;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;

/**
 * GELFMessageFilterHook.java: Feb 9, 2011 
 *
 * Filters GELF messages based on regular expression.
 *
 * @author: Joshua Spaulding <joshua.spaulding@gmail.com>
 */
public class GELFMessageFilterHook implements MessagePreReceiveHookIF {

    /**
     * Process the hook.
     */
    public boolean process(Object message) {
		/**
		 * Convert message Object to string for regex match
		 */
    	String msg = new String(((GELFMessage) message).toString());
    	String regex = null;
    	Pattern pattern = null;
    	Matcher matcher = null;
    	
    	System.out.println(msg);
		 
    	int regex_count = Integer.parseInt(Main.regexConfig.getProperty("filter.out.gelf.count"));
    	
    	for( int i = 0; i < regex_count; i++) {
    		regex = Main.regexConfig.getProperty("filter.out.gelf.regex." + i);
    		pattern = Pattern.compile(regex);
    		matcher = pattern.matcher(msg);

    	   	if(matcher.matches()){
    	   		if(Main.debugMode)
    	   			Syslog.getInstance("udp").debug("Message Filtered :" + msg);
    			return true;
    	   	}
    	}
    	return false;
    }
}
