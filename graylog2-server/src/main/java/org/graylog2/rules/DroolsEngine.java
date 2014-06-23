/*
 * Copyright 2014 TORCH GmbH
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
 */
package org.graylog2.rules;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RulesEngine;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class DroolsEngine implements RulesEngine {
    private static final Logger log = LoggerFactory.getLogger(DroolsEngine.class);

    private final KieServices kieServices;
    private KieContainer kieContainer;
    private final AtomicReference<KieSession> session = new AtomicReference<>();
    private int version = 1;

    private final Collection<String> rules = Lists.newArrayList();

    @Inject
    public DroolsEngine() {
        kieServices = KieServices.Factory.get();
    }

    @Override
    public boolean addRule(String ruleSource) {
        rules.add(ruleSource);
        return deployRules(increaseRulesVersion());
    }

    private ReleaseId increaseRulesVersion() {
        return kieServices.newReleaseId("org.graylog2", "dynamic-rules", Integer.toString(version++));
    }

    private boolean deployRules(ReleaseId newReleaseId) {
        final KieModule module = createAndDeployJar(kieServices, newReleaseId, rules.toArray(new String[rules.size()]));
        if (module == null) {
            return false;
        }
        if (kieContainer == null) {
            kieContainer = kieServices.newKieContainer(newReleaseId);
            final KieSession session = kieContainer.newKieSession();
            this.session.set(session);
            session.setGlobal("log", log);
        }
        kieContainer.updateToVersion(newReleaseId);

        return true;
    }

    @Override
    public boolean addRulesFromFile(String rulesFile) {
        try {
            final String rulesSource = Files.toString(new File(rulesFile), Charsets.UTF_8);
            rules.add(rulesSource);
            return deployRules(increaseRulesVersion());
        } catch (IOException e) {
            log.warn("Could not read drools source file. Not loading rules.", e);
        }
        return false;
    }

    @Override
    public int evaluate(Message message) {
        final KieSession kieSession = session.get();
        if (kieSession != null) {
            kieSession.insert(message);
            return kieSession.fireAllRules();
        }
        return 0;
    }

    public static KieModule createAndDeployJar( KieServices ks,
                                                ReleaseId releaseId,
                                                String... drls ) {
        byte[] jar = createKJar( ks, releaseId, null, drls );
        return deployJar( ks, jar );
    }
    public static byte[] createKJar(KieServices ks,
                                    ReleaseId releaseId,
                                    String pom,
                                    String... drls) {
        KieFileSystem kfs = ks.newKieFileSystem();
        if( pom != null ) {
            kfs.write("pom.xml", pom);
        } else {
            kfs.generateAndWritePomXML(releaseId);
        }
        for (int i = 0; i < drls.length; i++) {
            if (drls[i] != null) {
                kfs.write("src/main/resources/r" + i + ".drl", drls[i]);
            }
        }
        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if( kb.getResults().hasMessages( org.kie.api.builder.Message.Level.ERROR ) ) {
            for( org.kie.api.builder.Message result : kb.getResults().getMessages() ) {
                log.warn("Compiling rule failed: {}", result.getText());
            }
            return null;
        }
        InternalKieModule kieModule = (InternalKieModule) ks.getRepository()
                .getKieModule(releaseId);
        byte[] jar = kieModule.getBytes();
        return jar;
    }

    public static KieModule deployJar(KieServices ks, byte[] jar) {
        // Deploy jar into the repository
        Resource jarRes = ks.getResources().newByteArrayResource(jar);
        KieModule km = ks.getRepository().addKieModule(jarRes);
        return km;
    }
}
