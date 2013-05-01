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
package org.graylog2.periodical;

import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.SystemSettingAccessor;
import org.graylog2.plugin.Tools;
import org.graylog2.alarms.MessageCountAlarm;
import org.graylog2.alarms.StreamAlarmChecker;
import org.graylog2.plugin.alarms.Alarm;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AlarmScannerThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AlarmScannerThread.class);
    
    public static final int INITIAL_DELAY = 10;
    public static final int PERIOD = 60;
    
    private final Core graylogServer;
    
    public AlarmScannerThread(Core graylogServer) {
        this.graylogServer = graylogServer;
    }
    
    @Override
    public void run() {
        Map<String, Object> onlyAlerted = Maps.newHashMap();
        onlyAlerted.put("alarm_active", true);
        
        List<Stream> streams = StreamImpl.loadAllEnabled(graylogServer, onlyAlerted);
        
        if (streams.isEmpty()) {
            LOG.debug("No alertable streams found. Not doing anything more.");
            return;
        }
                
        for (Stream streamIF : streams) {
            StreamImpl stream = (StreamImpl) streamIF;
            StreamAlarmChecker checker = new StreamAlarmChecker(graylogServer, stream); 

            // Skip if limit and timespan have been configured for this stream.
            if (!checker.fullyConfigured()) {
                LOG.debug("Skipping alarm scan for stream <{}> - Timespan or limit not set.", stream.getId());
                continue;
            }
            
            // Is the stream over limit?
            if (checker.overLimit()) {
                // Are we still in grace period?
                if (stream.inAlarmGracePeriod()) {
                    LOG.debug("Stream <{}> is over alarm limit but in grace period. Skipping.", stream.getId());
                    continue;
                }
                
                int messageCount = checker.getMessageCount();
                
                LOG.debug("Stream <{}> is over alarm limit. Sending alerts.", stream.getId());
                
                // Update last alarm timestamp.
                stream.setLastAlarm(Tools.getUTCTimestamp(), graylogServer);
                
                MessageCountAlarm alarm = new MessageCountAlarm(stream, stream.getAlarmReceivers(graylogServer));
                alarm.setMessageCount(messageCount);
                alarm.setTopic("Stream message count alert: [" + stream.getTitle() + "]");
                alarm.setDescription("Stream [" + stream.getTitle() + "] received " + messageCount
                        + " messages in the last " + stream.getAlarmTimespan() + " minutes."
                        + " Limit: " + stream.getAlarmMessageLimit());

                // Send using all transports.
                sendMessages(alarm, stream);

                // Call all callbacks. Brace, brace, brace!
                callCallbacks(alarm, stream);

                
            } else {
                LOG.debug("Stream <{}> is not over alarm limit.", stream.getId());
            }

        }
    }
    
    private void sendMessages(Alarm alarm, Stream stream) {
        for (Transport transport : graylogServer.getTransports()) {
            // Check if this transport has users that configured it at all.
            if (alarm.getReceivers(transport).isEmpty()) {
                LOG.debug("Skipping transport [{}] because it has no configured users.", transport.getName());
                continue;
            }

            LOG.debug("Sending alarm for user <{}> via Transport [{}].", stream.getId(), transport.getName());
            transport.transportAlarm(alarm);
        }
    }
    
    private void callCallbacks(Alarm alarm, StreamImpl stream) {
        SystemSettingAccessor ssa = new SystemSettingAccessor(graylogServer);
        
        for (AlarmCallback callback : graylogServer.getAlarmCallbacks()) {
            String typeclass = callback.getClass().getCanonicalName();
            
            // Only call if callback is forced for all streams or enabled for this particular stream.
            if (ssa.getForcedAlarmCallbacks().contains(typeclass) || stream.getAlarmCallbacks().contains(typeclass)) {
                LOG.debug("Calling alarm callback [{}].", typeclass);
                try {
                    callback.call(alarm);
                } catch (AlarmCallbackException e) {
                    LOG.error("Execution of alarm callback [" + typeclass + "] failed.", e);
                }
            } else {
                LOG.debug("Skipping alarm callback [{}] because it has no configured streams.", callback.getName());
            }
        }
    }
    
}
