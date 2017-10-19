/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ihcserver;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Paul Dhaene
 */
public class Helpers {
    public static String randomUUID() {
	String[] s = new String[36];
	String itoh = "0123456789ABCDEF";
	// Make array of random hex digits. The UUID only has 32 digits in it, but we
	// allocate an extra items to make room for the '-'s we'll be inserting.
	for (int i = 0; i < 36; i++) {
	    s[i] = String.valueOf((int) Math.floor(Math.random() * 0x10));
	}
	// Conform to RFC-4122, section 4.4
	s[14] = String.valueOf(4);  // Set 4 high bits of time_high field to version
	int v19 = Integer.parseInt(s[19]);
	v19 = (v19 & 0x3) | 0x8;  // Specify 2 high bits of clock sequence
	s[19] = String.valueOf(v19);

	// Convert to hex chars
	for (int i = 0; i < 36; i++) {
	    s[i] = String.valueOf(itoh.charAt(Integer.parseInt(s[i])));
	}

	// Insert '-'s
	s[8] = s[13] = s[18] = s[23] = "-";

	//String uuid = "";
        StringBuilder uuid = new StringBuilder();
	for (String str : s) {
	    uuid.append(str);
	}
	return uuid.toString();
    }
    
    public static String encryptPassword(String password) {
	String sha1 = "";
	try {
	    MessageDigest crypt = MessageDigest.getInstance("SHA-1");
	    crypt.reset();
	    crypt.update(password.getBytes("UTF-8"));
	    sha1 = byteToHex(crypt.digest());
	} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
	}
	return sha1;
    }
    
    private static String byteToHex(final byte[] hash) {
	String result;
	try (Formatter formatter = new Formatter()) {
	    for (byte b : hash) {
		formatter.format("%02x", b);
	    }   result = formatter.toString();
	}
	return result;
    }
   
}
