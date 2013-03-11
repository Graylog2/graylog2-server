/**
 * Copyright 2011 Joshua Spaulding, Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.graylog2.plugin.RulesEngine;
import org.graylog2.plugin.Message;

public final class RulesEngineImpl implements RulesEngine {

    private static final Logger LOG = LoggerFactory.getLogger(RulesEngineImpl.class);
	
	private KnowledgeBuilder kbuilder = null;
	private KnowledgeBase kbase = null;
	
	public RulesEngineImpl() {
		this.kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		this.kbase = KnowledgeBaseFactory.newKnowledgeBase();
	}
	
	public void addRules(String rulesFile) {
		this.kbuilder.add(ResourceFactory.newFileResource(rulesFile), ResourceType.DRL);
		KnowledgeBuilderErrors errors = kbuilder.getErrors();
		if (errors.size() > 0) {
			for (KnowledgeBuilderError error: errors) {
				LOG.error(error.toString());
			}
			throw new IllegalArgumentException("Could not parse knowledge.");
		}
		this.kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
	}
	
	public void evaluate(Message message) {
		// Create new session, insert fact and evaluate
		StatefulKnowledgeSession ksession = this.kbase.newStatefulKnowledgeSession();
		ksession.insert(message);
		ksession.fireAllRules();
		ksession.dispose();
	}
}
