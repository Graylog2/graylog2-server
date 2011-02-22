package org.graylog2.messagehandlers.common;

import static org.junit.Assert.*;

import org.graylog2.RulesEngine;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.junit.Before;
import org.junit.Test;


public class MessageKnowledgeBaseTest {
	private GELFMessage message = null;
	private RulesEngine drools = null;
	
	@Before
	public void setup() throws Exception {
		this.message = new GELFMessage();
		this.message.setHost("localhost");
		this.message.setVersion("1.0");
		this.message.setShortMessage("Test");
		this.message.setFacility("junit-test");
		testKsessionRuleFire();
	}
	
	public void testKsessionRuleFire() throws Exception {
		this.drools = new RulesEngine();
		drools.addRules("misc/graylog2.drl");
	}
	
	@Test
	public void testOverwriteRuleFile() throws Exception {
		this.drools.evaluate(this.message);
		assertEquals(message.getHost(), "localhost.example.com");
	}
	
	@Test
	public void testFilteroutRuleFile() throws Exception {
		GELFMessage message = new GELFMessage();
		message.setHost("www1");
		message.setFacility("graylog2-test");
		message.setVersion("1.0");
		message.setShortMessage("Test");
		this.drools.evaluate(message);
		assertTrue(message.getFilterOut());
	}
	
	@Test
	public void testRegularExpressionFilteroutRuleFile() throws Exception {
		GELFMessage message = new GELFMessage();
		message.setHost("firewall");
		message.setFacility("graylog2-test");
		message.setVersion("1.0");
		message.setShortMessage("Test");
		message.setFullMessage("Received ICMP Packet from firewall");
		this.drools.evaluate(message);
		assertTrue(message.getFilterOut());	
	}
}
