/**
 * Copyright 2011 Rackspace Hosting Inc.
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

package org.graylog2.messagehandlers.scribe;

import org.apache.log4j.Logger;
import org.graylog2.messagehandlers.gelf.InvalidGELFCompressionMethodException;
import org.graylog2.messagehandlers.gelf.SimpleGELFClientHandler;
import org.graylog2.messagehandlers.syslog.GraylogSyslogServerEvent;
import org.graylog2.messagehandlers.syslog.SyslogEventHandler;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerIF;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.zip.DataFormatException;
import java.util.List;

import scribe.thrift.scribe.Iface;
import scribe.thrift.LogEntry;
import scribe.thrift.ResultCode;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.thrift.TException;

/**
 * ScribeHandler.java:
 *
 * Object responsible for handling a Scribe request containing a list of GELF entries
 *
 */
public class ScribeHandler implements Iface {

    private static final Logger LOG = Logger.getLogger(ScribeHandler.class);

    public ScribeHandler() {
    }
    
    @Override
    public ResultCode Log(List<LogEntry> messages) throws TException {
        LOG.info("Received " + messages.size() + " messages.");
                
        for (LogEntry message : messages) {
            LOG.trace("received new scribe message: category= " + message.category + " message= " + message.message);
            try {
                handleMessage(message.message);
            } catch (Exception ex) {
                LOG.error("Failed to process message: category= " + message.category + " message= " + message.message);
                ex.printStackTrace();
                throw new RuntimeException("Exception processing event", ex);
            }
        }
        
        return ResultCode.OK;
    }
    
    private void handleMessage(String msgBody) throws DataFormatException, UnsupportedEncodingException, InvalidGELFCompressionMethodException, IOException {
        // Handle GELF message.
        SimpleGELFClientHandler gelfHandler = new SimpleGELFClientHandler(new String(msgBody));
        gelfHandler.handle();
    }
}
