/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
