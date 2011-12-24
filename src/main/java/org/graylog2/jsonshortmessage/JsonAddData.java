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
import org.apache.log4j.Logger;
// import org.graylog2.Main;
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
	private static final Logger LOG = Logger.getLogger(JsonAddData.class);
	private static boolean enableJsonAddData = false;
	private static HashSet<String> dropList = null;
	
	public JsonAddData() {
	}
	
	public static void setEnableJsonAddData(Boolean b) {
		enableJsonAddData = b;
	}
	
	public static boolean getEnableJsonAddData() {
		return enableJsonAddData;
	}
	
	public void setJsonAddDataFilter(String fileName) throws IOException {
		String dropString;
		dropList = new HashSet<String>(100);
		
		if (fileName == null) {
			new IOException("Received null string for drop list file name");
			return;
		}
			
		try {
			BufferedReader filterFile = new BufferedReader(new FileReader(fileName));
			while ((dropString = filterFile.readLine()) != null) {
				dropList.add(dropString.trim());
			}
		} catch (IOException e) {
			new IOException("Failed reading Key dropping list for JSON-based parsing of shortMessage for Additional Data");
		}
	}
	
	private static boolean isDrop(String candidate) {
		if (dropList == null || dropList.isEmpty()) {
			return false;
		}
		return dropList.contains(candidate);
	}
		
	public static void amplify(GELFMessage target) {
		Map<String,Object> localData = null;
		
		localData = parseJsonString(target.getShortMessage());
		if(localData == null || localData.isEmpty()) {
			return;
		}
		stuffGELF(target, localData);
	}
	
	private static Map<String,Object> parseJsonString(String jsonString) {
	    HashMap<String,Object> result = new HashMap<String,Object>(100);
	    JSONParser parser = new JSONParser();
	    Object parsedJson = new Object();
	    
	    if (jsonString == null || jsonString.isEmpty()) {
	    	LOG.error("parseJsonString got bad JSON string to parse");
	    	result.clear();
	    	return result;
	    }
	    try {
	        parsedJson = parser.parse(jsonString);
	        LOG.debug("parsedJson is "+parsedJson.getClass());
	    } catch (ParseException pe) {
	    	LOG.error("Caught parsing exception in: "+jsonString);
	    	result.clear();
	    	return result;
	    }
	    
	    try {
	    	if (parsedJson.getClass().equals(JSONObject.class)) {
	    		// result.putAll((Map<String,Object>) parsedJson);
	    		result.putAll(walkJsonMap((JSONObject) parsedJson));
	    	} else if (parsedJson.getClass().equals(JSONArray.class)) {
	 	    	result.putAll(walkJsonArray("root", (JSONArray) parsedJson));
	    	}
	    } catch (Exception e) {
	    	LOG.warn("Problem saving parsed JSON Map or Array");
	    	result.clear();
	    }
	    return result;
	}
	    
	private static Map<String,Object> walkJsonMap(JSONObject jsonToWalk) {
		HashMap<String,Object> walkMapResult = new HashMap<String,Object>(20);
		if (jsonToWalk == null || jsonToWalk.isEmpty()) {
			return walkMapResult;
		}

		Map<String,Object> walkMap = jsonToWalk;

		try {
			for (Map.Entry<String, Object> mapEntry : walkMap.entrySet()) {
				if (!dropList.isEmpty() && isDrop(mapEntry.getKey())) {
					continue;
				}
				walkMapResult.put(mapEntry.getKey(), mapEntry.getValue());
			
				if (mapEntry.getValue().getClass().equals(JSONObject.class)) {
					walkMapResult.putAll(walkJsonMap((JSONObject) mapEntry.getValue()));
				} else if (mapEntry.getValue().getClass().equals(JSONArray.class)) {
					walkMapResult.putAll(walkJsonArray((String) mapEntry.getKey(), (JSONArray) mapEntry.getValue()));
				}
			}
	    } catch (Exception e) {
	    	LOG.warn("Problem walking JSON object and converting to map");
	    	walkMapResult.clear();
	    }
	    return walkMapResult;
	}
	
	private static Map<String,Object> walkJsonArray(String arrayLabel, JSONArray jsonArray) {
		HashMap<String,Object> walkArrayResult = new HashMap<String,Object>(20);
		if (jsonArray.isEmpty() || arrayLabel.equals("")) {
			return walkArrayResult;
		}
		try {
			ListIterator<JSONArray> iterator = jsonArray.listIterator();
			while (iterator.hasNext()) {
				String arrayKey = arrayLabel+"_"+iterator.nextIndex();
				Object arrayElement = iterator.next();
				
				walkArrayResult.put(arrayKey, arrayElement);
				if (arrayElement.getClass().equals(JSONObject.class)) {
					walkArrayResult.putAll(walkJsonMap((JSONObject) arrayElement));
				} else if (arrayElement.getClass().equals(JSONArray.class)) {
					walkArrayResult.putAll(walkJsonArray(arrayLabel, (JSONArray) arrayElement));
				}
			}
		} catch (Exception e) {
			LOG.warn("Problem walking JSON array and adding to map with keys");
			walkArrayResult.clear();
		}
	    return walkArrayResult;
	}

	private static void stuffGELF(GELFMessage target, Map<String, Object> parsedLocalData) {
		// iterate over local Map and use message.addAdditionalData to provide to GELF message
		try {
			for (Map.Entry<String, Object> entry : parsedLocalData.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				// Stuff values from localData into GELF additionalData
				target.addAdditionalData("_"+key, value);
			}
		} catch (Exception e) {
			LOG.warn("Problem adding parsed Additional Data to GELF message");
		}
	}
}
