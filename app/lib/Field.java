package lib;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;

import com.google.common.collect.ImmutableSet;

public class Field {

	private final String name;
	private final String hash;
	
	private final static Set<String> STANDARD_SELECTED_FIELDS = ImmutableSet.of(
			"source",
			"message"
	);
	
	public Field(String field) {
		this.name = field;
		
		try {
			/* 
			 * Using MD5 instead of Base64 for now because the = chars in Base64 fuck up HTML attributes.
			 * Possibly replace with something faster but I don't really care for the hand full of field names.
			 */
			MessageDigest d = MessageDigest.getInstance("MD5");
			this.hash = new String(Hex.encodeHex(d.digest(field.getBytes())));
		} catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); /* ... well ... */ }
	}
	
	public String getName() {
		return name;
	}
	
	public String getHash() {
		return hash;
	}
	
	public boolean isStandardSelected() {
		return STANDARD_SELECTED_FIELDS.contains(name);
	}
	
}
