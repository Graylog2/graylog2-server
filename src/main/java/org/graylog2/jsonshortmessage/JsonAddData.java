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

import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
		// String nxAppName = target.getFacility();
		// String nxMessType = target.getFile();
		Map<String, Object> localData;
		
		localData = nxParseJsonString(target.getShortMessage());
		if(localData.isEmpty()) {
			return;
		}
		stuffGELF(target, localData);
	}
	
	private static Map<String,Object> nxParseJsonString(String jsonString) {
	    HashMap<String,Object> result = new HashMap<String,Object>(100);

	    JSONParser parser = new JSONParser();
	    Object parsedJson = new Object();
	    try {
	        parsedJson = parser.parse(jsonString);
	        // System.out.println("parsedJson is "+parsedJson.getClass());
	    } catch (ParseException e) {
	    	System.out.println("Caught parsing exception");
	    	result.clear();
	    	return result;
	    }
	    
	    if (parsedJson.getClass().equals(JSONObject.class)) {
	    	result.putAll((Map<String,Object>) parsedJson);
	    	System.out.println("JSON object => "+parsedJson);
	    	result.putAll(walkJsonMap((JSONObject) parsedJson));
	    } else if (parsedJson.getClass().equals(JSONArray.class)) {
	    	System.out.println("JSON array => "+parsedJson);
	    	result.putAll(walkJsonArray("root", (JSONArray) parsedJson));
	    } else {
	    	// System.out.println("Parser returned object not in class JSONOBject or JSONArray. This should never happen.");
	    }
	    return result;
	}
	    
	private static Map<String,Object> walkJsonMap(JSONObject jsonToWalk) {
		HashMap<String,Object> walkMapResult = new HashMap<String,Object>(20);
		if (jsonToWalk.isEmpty()) {
			return walkMapResult;
		}

		Map<String,Object> walkMap = jsonToWalk;
		// System.out.println(walkMap.entrySet());
	    for (Map.Entry<String, Object> mapEntry : walkMap.entrySet()) {
	        System.out.println(mapEntry.getKey()+" => "+mapEntry.getValue());

			walkMapResult.put(mapEntry.getKey(), mapEntry.getValue());
			
			if (mapEntry.getValue().getClass().equals(JSONObject.class)) {
				// System.out.println("walkJasonMap found a JSONObject");
				walkMapResult.putAll(walkJsonMap((JSONObject) mapEntry.getValue()));
			} else if (mapEntry.getValue().getClass().equals(JSONArray.class)) {
				// System.out.println("walkJasonMap found a JSONArray");
				walkMapResult.putAll(walkJsonArray((String) mapEntry.getKey(), (JSONArray) mapEntry.getValue()));
			} else {
				// System.out.println("walkJasonMap found a type not requiring further processing");
			}
	    }
	    return walkMapResult;
	}
	
	private static Map<String,Object> walkJsonArray(String arrayLabel, JSONArray jsonArray) {
		HashMap<String,Object> walkArrayResult = new HashMap<String,Object>(20);
		if (jsonArray.isEmpty() || arrayLabel.equals("")) {
			return walkArrayResult;
		}
		ListIterator<JSONArray> iterator = jsonArray.listIterator();
		while (iterator.hasNext()) {
			String arrayKey = arrayLabel+"_"+iterator.nextIndex();
			Object arrayElement = iterator.next();
			System.out.println(arrayKey+" => "+arrayElement);
			walkArrayResult.put(arrayKey, arrayElement);
			if (arrayElement.getClass().equals(JSONObject.class)) {
			 	walkArrayResult.putAll(walkJsonMap((JSONObject) arrayElement));
			} else if (arrayElement.getClass().equals(JSONArray.class)) {
				walkArrayResult.putAll(walkJsonArray(arrayLabel, (JSONArray) arrayElement));
			} else {
				// System.out.println("walkJSONArray found a type not requiring further processing");
			}

		}
	    return walkArrayResult;
	}

	private static void stuffGELF(GELFMessage target, Map<String, Object> nxParsedJSON) {
		// iterate over local Map and use message.addAdditionalData to provide to GELF message
		for (Map.Entry<String, Object> entry : nxParsedJSON.entrySet()) {
		    String key = entry.getKey();
		    Object value = entry.getValue();
		    // Stuff values from localData into GELF additionalData
		    target.addAdditionalData("_"+key, value);
		}		
	}
}
