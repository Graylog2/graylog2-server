/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.plugin.streams;

import org.bson.types.ObjectId;

/**
 *
 * @author lennart.koopmann
 */
public interface StreamRule {

    public ObjectId getObjectId();

    public int getRuleType();

    public String getValue();
    
}
