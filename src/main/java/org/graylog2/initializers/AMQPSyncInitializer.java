/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.initializers;

import org.graylog2.Core;
import org.graylog2.periodical.AMQPSyncThread;

/**
 *
 * @author lennart.koopmann
 */
public class AMQPSyncInitializer extends SimpleFixedRateScheduleInitializer implements Initializer {
    
    public AMQPSyncInitializer(Core graylogServer) {
        this.graylogServer = graylogServer;
    }

    @Override
    public void initialize() {
        configureScheduler(
                new AMQPSyncThread(this.graylogServer),
                AMQPSyncThread.INITIAL_DELAY,
                AMQPSyncThread.PERIOD
        );
    }
    
    @Override
    public boolean masterOnly() {
        return true;
    }
    
}
