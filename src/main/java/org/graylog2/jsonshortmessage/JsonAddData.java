/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2.jsonshortmessage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.graylog2.messagehandlers.gelf.GELFMessage;

/**
 * JsonAddData.java: Dec 22, 2011
 *
 * Extract JSON key value pairs in shortMessage part
 * of GELF message
 *
 * @author Bob Webber <webber@panix.com>
 * 
 */


public class JsonAddData {
	private static boolean enableJsonAddData = false;
	private static HashSet<String> dropList = null;
	
	public JsonAddData() {
		enableJsonAddData = true;
	}
	
	public static boolean useJsonAddData() {
		return enableJsonAddData;
	}
	
	public void setJsonAddDataFilter(String fileName) {
		String dropString;
		dropList = new HashSet<String>(100);
		
		if (fileName == null) {
			return;
		}
			
		try {
			BufferedReader filterFile = new BufferedReader(new FileReader(fileName));
			while ((dropString = filterFile.readLine()) != null) {
				dropList.add(dropString.trim());
			}
		} catch (IOException e) {
			// do something about bad file
		}
	}
	
	private static boolean isDrop(String candidate) {
		if (dropList.isEmpty()) {
			return false;
		}
		if (dropList.contains(candidate)) {
			return true;
		}
		return false;
	}
		
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
	    } catch (ParseException pe) {
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
	        if (isDrop(mapEntry.getKey())) {
	        	continue;
	        }
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
