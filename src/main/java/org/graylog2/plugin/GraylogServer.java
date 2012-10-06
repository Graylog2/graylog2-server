/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.plugin;

import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.database.HostCounterCache;

/**
 *
 * @author lennart.koopmann
 */
public interface GraylogServer extends Runnable {

    public Buffer getProcessBuffer();

    public Buffer getOutputBuffer();
    
    public boolean isMaster();
    
    public String getServerId();
    
    public RulesEngine getRulesEngine();
 
    public MessageCounterManager getMessageCounterManager();
    
    public HostCounterCache getHostCounterCache();
    
}
