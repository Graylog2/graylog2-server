package org.graylog2.messagehandlers.common;

import org.graylog2.RulesEngine;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.junit.Before;
import org.junit.Test;


public class MessageKnowledgeBaseTest {
	private GELFMessage message = null;
	
	@Before
	public void setup() throws Exception {
		this.message = new GELFMessage();
		this.message.setHost("localhost");
		this.message.setVersion("1.0");
		this.message.setShortMessage("Test");
		this.message.setFacility("junit-test");
	}
	
	@Test
	public void testKsessionRuleFire() throws Exception {
		RulesEngine drools = new RulesEngine();
		drools.addRules("misc/graylog2.drl");
		drools.evaluate(this.message); 
	}

}
