/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.plugin;

import java.util.Map;

/**
 *
 * @author lennart.koopmann
 */
public interface MessageCounterManager {
    
    public void register(String name);

    public MessageCounter get(String name);
    
    public Map<String, MessageCounter> getAllCounters();
    
}
