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

package org.graylog2.gelf;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

import java.io.IOException;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.Core;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.logmessage.LogMessage;

import java.util.concurrent.TimeUnit;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GELFProcessor.class);
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private Core server;
    private final Meter incomingMessages = Metrics.newMeter(GELFProcessor.class, "IncomingMessages", "messages", TimeUnit.SECONDS);
    private final Meter incompleteMessages = Metrics.newMeter(GELFProcessor.class, "IncompleteMessages", "messages", TimeUnit.SECONDS);
    private final Meter processedMessages = Metrics.newMeter(GELFProcessor.class, "ProcessedMessages", "messages", TimeUnit.SECONDS);
    private final Timer gelfParsedTime = Metrics.newTimer(GELFProcessor.class, "GELFParsedTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    public GELFProcessor(Core server) {
        this.server = server;
    }

    public void messageReceived(GELFMessage message) throws BufferOutOfCapacityException {
        incomingMessages.mark();
        
        LogMessage lm;
        
        // Convert to LogMessage
        lm = parse(message.getJSON());
        
        if (!lm.isComplete()) {
            incompleteMessages.mark();
            LOG.debug("Skipping incomplete message.");
        }

        // Add to process buffer.
        LOG.debug("Adding received GELF message <{}> to process buffer: {}", lm.getId(), lm);
        processedMessages.mark();
        server.getProcessBuffer().insertCached(lm);
    }
    
    private LogMessage parse(String message) {
    	TimerContext tcx = gelfParsedTime.time();
    	
    	try {
	    	LogMessage lm = new LogMessage();
	    	JsonParser parser = JSON_FACTORY.createJsonParser(message);
	    	
	    	JsonToken token = parser.nextToken();
			if (JsonToken.START_ARRAY == token) {
				// Skip to first array element
				token = parser.nextToken();
			}

			if(JsonToken.START_OBJECT != token) {
				throw new IllegalStateException("Expected either a JSON object, or an array containing at least one JSON object");
			}
	    	
	    	while (JsonToken.END_OBJECT != (token = parser.nextToken())) {
	    		String key = parser.getCurrentName();
	    		token = parser.nextToken();
	    		
	    		if("host".equals(key)) {
	    			lm.setHost(parser.getValueAsString());
	    		} else if("short_message".equals(key)) {
	    			lm.setShortMessage(parser.getValueAsString());
	    		} else if("full_message".equals(key)) {
	    			lm.setFullMessage(parser.getValueAsString());
	    		} else if("file".equals(key)) {
	    			lm.setFile(parser.getValueAsString());
	    		} else if("line".equals(key)) {
	    			lm.setLine(parser.getIntValue());
	    		} else if("level".equals(key)) {
	    			lm.setLevel(parser.getIntValue());
	    		} else if("facility".equals(key)) {
	    			lm.setFacility(parser.getValueAsString());
	    		} else if("timestamp".equals(key)) {
	    			lm.setCreatedAt(parser.getDoubleValue());
	    		} else if(key.startsWith(GELFMessage.ADDITIONAL_FIELD_PREFIX)) {
	    			if(key.equals("_id")) {
	                    LOG.warn("Client tried to override _id field! Skipped field, but still storing message.");
	                    continue;
	    			}
	    			CharSequence value;
	    			switch(token) {
	    				case START_ARRAY: {
	    					StringWriter writer = new StringWriter();
	    					JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
	    					generator.writeStartArray();
							tokenStreamToJSONString(parser, generator, JsonToken.END_ARRAY);
							generator.writeEndArray();
	    					generator.close();
	    					value = writer.toString();
	    					break;
	    				}
	    				case START_OBJECT: {
	    					StringWriter writer = new StringWriter();
	    					JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
	    					generator.writeStartObject();
							tokenStreamToJSONString(parser, generator, JsonToken.END_OBJECT);
							generator.writeEndObject();
	    					generator.close();
	    					value = writer.toString();
	    					break;
	    				}
	    				default:
	    					value = parser.getValueAsString();
	    					break;
	    			}
	    			lm.setAdditionalData(key,  value.toString());
	    		}
	    	}
	    	
	    	if(lm.getCreatedAt() <= 0) {
	    		lm.setCreatedAt(Tools.getUTCTimestampWithMilliseconds());
	    	}
	    	if(lm.getFacility() == null) {
				lm.setFacility(LogMessage.STANDARD_FACILITY);
	    	}
	    	if(lm.getLevel() <= 0) {
				lm.setLevel(LogMessage.STANDARD_LEVEL);
	    	}

	    	return lm;
    	} catch(IOException e) {
    		throw new IllegalStateException("JSON is null/could not be parsed (invalid JSON)", e);
    	} finally {
    		tcx.stop();
    	}
    }

	private void tokenStreamToJSONString(JsonParser parser, JsonGenerator generator, JsonToken stopToken) throws IOException,
			JsonParseException, JsonGenerationException
	{
		JsonToken token;
		while (stopToken != (token = parser.nextToken())) {
    		switch(token) {
    			case VALUE_NULL:
    				generator.writeNull();
    				break;
    			case VALUE_TRUE:
    			case VALUE_FALSE:
    				generator.writeBoolean(parser.getBooleanValue());
    				break;
    			case VALUE_NUMBER_FLOAT:
    				generator.writeNumber(parser.getValueAsDouble());
    				break;
    			case VALUE_NUMBER_INT:
    				generator.writeNumber(parser.getValueAsLong());
    				break;
    			case VALUE_STRING:
    				generator.writeString(parser.getValueAsString());
    				break;
    			case START_ARRAY:
    				generator.writeStartArray();
					tokenStreamToJSONString(parser, generator, JsonToken.END_ARRAY);
					generator.writeEndArray();
    				break;
    			case START_OBJECT:
    				generator.writeStartObject();
					tokenStreamToJSONString(parser, generator, JsonToken.END_OBJECT);
					generator.writeEndObject();
    				break;
    			case FIELD_NAME:
    				generator.writeFieldName(parser.getCurrentName());
    				break;
    			default:
    				throw new IllegalStateException("Unexpected token in JSON/GELF: " + token);
    		}
    	}
	}
}
