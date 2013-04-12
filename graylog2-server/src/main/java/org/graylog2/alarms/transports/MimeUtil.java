/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.alarms.transports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Date;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

public class MimeUtil {

    private MimeUtil() {}
    
    static void sendMessage(
            Session session,
            Transport transport,
            InternetAddress from,
            InternetAddress receiver,
            String subject,
            String messageText,
            String contentType) throws MessagingException, IOException {
    
        byte[] messageBytes = messageText.getBytes(Charset.forName("UTF-8"));
        String encoding = selectMimeEncoding(messageBytes, contentType);
        InternetHeaders headers = buildInternetHeaders(contentType, encoding);
        byte[] encodedBytes = encodeBytesToMime(messageBytes, encoding);
        
        MimeMultipart mp = new MimeMultipart();
        MimeBodyPart part = new MimeBodyPart(headers, encodedBytes);
        mp.addBodyPart(part);
        
        MimeMessage message = new MimeMessage(session);
        
        message.setFrom(from);
        message.setRecipient(MimeMessage.RecipientType.TO, receiver);
        message.setSubject(subject, "UTF-8");
        message.setContent(mp);
        message.setSentDate(new Date());
        message.saveChanges();
        
        transport.sendMessage(message, message.getAllRecipients());
    }
    
    private static String selectMimeEncoding(byte[] messageBytes, String contentType) {
        DataSource dataSource = new ByteArrayDataSource(messageBytes, contentType);
        return MimeUtility.getEncoding(dataSource);
    }

    private static InternetHeaders buildInternetHeaders(String contentType, String encoding) {
        InternetHeaders headers = new InternetHeaders();
        headers.setHeader("Content-Type", contentType + "; charset=UTF-8");
        headers.setHeader("Content-Transfer-Encoding", encoding);
        return headers;
    }
    
    private static byte[] encodeBytesToMime(byte[] messageBytes, String encoding) throws MessagingException, IOException {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        OutputStream encoder = MimeUtility.encode(encoded, encoding);
        encoder.write(messageBytes);
        encoder.close();
        return encoded.toByteArray();
    }
}
