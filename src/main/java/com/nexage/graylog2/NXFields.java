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

import java.util.Map;
import java.util.HashMap;
import org.json.simple.*;
import org.graylog2.messagehandlers.gelf.GELFMessage;

/**
 * NXFields.java: Dec 20, 2011
 *
 * Extract JSON key value pairs in Nexage application messages
 *
 * @author Bob Webber <bob.webber@nexage.com>
 */


public class NXFields {
	// private static Map<String, Object> localData = new HashMap<String, Object>(50);
	// use facility string to identify Nexage application
	// private String nxAppName;
	// use file name string to identify type of message from application
	// private String nxMessType;
		
	public static void amplify(GELFMessage target) {
		String nxAppName = target.getFacility();
		String nxMessType = target.getFile();
		HashMap<String, Object> localData = new HashMap<String, Object>(50);
		
		localData = nxParseJSON(target.getShortMessage());
		if(localData.isEmpty()) {
			return;
		}
		stuffGELF(target, localData);
	}
	
	private static HashMap<String, Object> nxParseJSON(String jsonString) {
	    HashMap<String, Object> result = null;	
	    
	    return result;
	}
		
	private static void stuffGELF(GELFMessage target, HashMap<String, Object> nxParsedJSON) {
		// iterate over local Map and use message.addAdditionalData to provide to GELF message
		for (Map.Entry<String, Object> entry : nxParsedJSON.entrySet()) {
		    String key = entry.getKey();
		    Object value = entry.getValue();
		    // Stuff values from localData into GELF additionalData
		    target.addAdditionalData("_"+key, value);
		}		
	}
}
