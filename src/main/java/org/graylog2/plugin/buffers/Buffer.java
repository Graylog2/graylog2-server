/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.plugin.buffers;

import org.graylog2.plugin.logmessage.LogMessage;

/**
 *
 * @author lennart.koopmann
 */
public interface Buffer {
    
    public void insert(LogMessage message);
    
}
