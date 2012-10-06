/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.plugin.database;

import java.util.Set;

/**
 *
 * @author lennart.koopmann
 */
public interface HostCounterCache {

    public void increment(String hostname);

    public void reset(String hostname);

    public int getCount(String hostname);

    public Set<String> getAllHosts();
    
}
