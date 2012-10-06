/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.plugin;

import java.util.Map;
import org.bson.types.ObjectId;

/**
 *
 * @author lennart.koopmann
 */
public interface MessageCounter {

    public int getTotalCount();

    public Map<String, Integer> getStreamCounts();

    public Map<String, Integer> getHostCounts();

    public int getThroughput();

    public int getHighestThroughput();

    public void resetAllCounts();

    public void resetHostCounts();

    public void resetStreamCounts();

    public void resetTotal();
    
    public void resetThroughput();

    public void incrementTotal();

    public void incrementThroughput();

    public void countUpTotal(final int x);

    public void countUpThroughput(final int x);

    public void incrementStream(final ObjectId streamId);

    public void countUpStream(final ObjectId streamId, final int x);

    public void incrementHost(final String hostname);

    public void countUpHost(String hostname, final int x);

}
