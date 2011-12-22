package com.nexage.graylog2;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import org.json.simple.JSONObject;
// import org.json.simple.JSONAware;
// import org.json.simple.JSONStreamAware;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Nexage_Graylog2_JSON_Parser_Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String s="{\"foo\":{\"1\":2},\"bar\":[0,{\"1\":{\"2\":{\"3\":{\"4\":[5,{\"6\":7}]}}}}]}";
		System.out.println("=======decode=======");
		Map<String,Object> sausage;
		sausage = nxParseJsonString(s);
		System.out.println(sausage);
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
}
