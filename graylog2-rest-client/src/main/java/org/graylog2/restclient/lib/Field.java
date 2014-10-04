/**
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
 */
package org.graylog2.restclient.lib;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class Field {

	private final String name;
	private final String hash;
	
	public final static Set<String> STANDARD_SELECTED_FIELDS = ImmutableSet.of(
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
