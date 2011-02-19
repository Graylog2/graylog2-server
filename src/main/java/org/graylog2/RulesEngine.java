package org.graylog2;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.graylog2.messagehandlers.gelf.GELFMessage;

public final class RulesEngine {
	
	private KnowledgeBuilder kbuilder = null;
	private KnowledgeBase kbase = null;
	
	public RulesEngine() {
		this.kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		this.kbase = KnowledgeBaseFactory.newKnowledgeBase();
	}
	
	public void addRules(String rulesFile) {
		this.kbuilder.add(ResourceFactory.newFileResource(rulesFile), ResourceType.DRL);
		KnowledgeBuilderErrors errors = kbuilder.getErrors();
		if (errors.size() > 0) {
			for (KnowledgeBuilderError error: errors) {
				System.err.println(error);
			}
			throw new IllegalArgumentException("Could not parse knowledge.");
		}
		this.kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
	}
	
	public void evaluate(GELFMessage message) {
		// Create new session, insert fact and evaluate
		StatefulKnowledgeSession ksession = this.kbase.newStatefulKnowledgeSession();
		ksession.insert(message);
		ksession.fireAllRules();
		ksession.dispose();
	}
}
