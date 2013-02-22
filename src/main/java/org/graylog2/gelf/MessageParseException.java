/*
 * @(#) MessageParseException.java
 * Created 20.02.2013 by oleg
 * (C) ONE, SIA
 */
package org.graylog2.gelf;

import java.io.IOException;

/**
 * To be thrown when GELF cannot understand input.
 * 
 * @author Oleg Anastasyev<oa@hq.one.lv>
 *
 */
public class MessageParseException extends IOException
{

    /**
     * 
     */
    public MessageParseException()
    {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public MessageParseException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param message
     */
    public MessageParseException(String message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public MessageParseException(Throwable cause)
    {
        super(cause);
    }
    
    

}
