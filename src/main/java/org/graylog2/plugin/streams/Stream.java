/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.plugin.streams;

import java.util.List;
import org.bson.types.ObjectId;

/**
 *
 * @author lennart.koopmann
 */
public interface Stream {

    public List<StreamRule> getStreamRules();
    
    public ObjectId getId();

    public String getTitle();

    @Override
    public String toString();
}
