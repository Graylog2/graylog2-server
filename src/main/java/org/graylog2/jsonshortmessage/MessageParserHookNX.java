/**
 * Copyright 2011 Bob Webber <bob.webber@nexage.com>
 *
 * This file is part of the Nexage platform extension to Graylog2.
 *
 * This extension to the Graylog2 free software is proprietary.
 * All rights to the software are reserved by Nexage, Inc. and the
 * original author. This software may not be reproduced or used as
 * the basis of a derived work without the express permission of the
 * copyright holders.
 * 
 */

package com.nexage.graylog2;

import org.graylog2.messagehandlers.common.MessagePreReceiveHookIF;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import com.nexage.graylog2.NXFields;

/**
 * MessageParserHookNX.java: Dec 20, 2011
 *
 * Extract JSON key value pairs in Nexage application messages
 *
 * @author Bob Webber <bob.webber@nexage.com>
 */
public class MessageParserHookNX implements MessagePreReceiveHookIF {

    /**
     * Process the hook.
     */

    public void process(GELFMessage message) {
		/**
		 * Check GELFMessage.shortMessage for actual content
		 */
    	if (message.getShortMessage().equals("")) {
    		return;
    	}
    	NXFields.amplify(message);
    }
}
