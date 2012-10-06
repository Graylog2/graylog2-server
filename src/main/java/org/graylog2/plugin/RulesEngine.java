/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.plugin;

import org.graylog2.plugin.logmessage.LogMessage;

/**
 *
 * @author lennart.koopmann
 */
public interface RulesEngine {
    
    public void addRules(String rulesFile);
	
    public void evaluate(LogMessage message);
    
}
