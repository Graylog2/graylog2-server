package com.nexage.graylog2;

import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Nexage_Graylog2_JSON_Parser_Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// String s="{\"foo\":{\"1\":2},\"bar\":[0,{\"1\":{\"2\":{\"3\":{\"4\":[5,{\"6\":7}]}}}}]}";
		String s = "{\"startedOn\":1324390232727,\"endedOn\":1324390232810,\"executionTimeInMillis\":83,\"requestData\":{\"tag\":\"8a809449013131b07f6bbe6bd77402c1\",\"mode\":\"live\",\"h\":480,\"w\":320,\"spcat\":\"IAB9,IAB1\",\"dip\":\"99.255.144.138\",\"dcountry\":\"CAN\",\"dua\":\"Mozilla/5.0 (iPod touch; U; CPU iPhone OS 5_0_1 like Mac OS X; en-us) AppleWebKit/528.18 (KHTML, like Gecko) Version/4.0 Mobile/7A341 Safari/528.16\",\"dmake\":\"Apple\",\"dmodel\":\"iPod Touch\",\"dos\":\"iOS\",\"dosv\":\"3.0\",\"dpid\":\"30f1418d2f02f90c17a1adbd1b8377cb8172741b\",\"prettyLog\":false,\"hasUserData\":false,\"hasDeviceData\":true},\"auctionParticipants\":{\"119\":\"Status : NOBID, Offered Price : 0\",\"288\":\"Status : NOBID, Offered Price : 0\",\"327\":\"Status : NOBID, Offered Price : 0\",\"223\":\"Status : NOBID, Offered Price : 0\",\"236\":\"Status : NOBID, Offered Price : 0\",\"54\":\"Status : NOBID, Offered Price : 0\",\"353\":\"Status : NOBID, Offered Price : 0\",\"67\":\"Status : NOBID, Offered Price : 0\",\"41\":\"Status : NOBID, Offered Price : 0\",\"210\":\"Status : NOBID, Offered Price : 0\",\"379\":\"Status : NOBID, Offered Price : 0\"},\"output\":\"\",\"methodProfileStats\":[]}";
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
