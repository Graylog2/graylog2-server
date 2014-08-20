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
package org.graylog2.rules;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RulesEngine;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.ReleaseId;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class DroolsEngine implements RulesEngine {
    private static final Logger LOG = LoggerFactory.getLogger(DroolsEngine.class);

    private final KieServices kieServices;
    private final Set<URL> builtinRuleUrls;
    private KieContainer kieContainer;
    private final AtomicReference<KieSession> session = new AtomicReference<>();

    private final List<String> liveRules = Lists.newArrayList();
    private int version = 0;
    private ReleaseId currentReleaseId;

    @Inject
    public DroolsEngine(Set<URL> builtinRuleUrls) {
        this.builtinRuleUrls = builtinRuleUrls;
        kieServices = KieServices.Factory.get();
        liveRules.add("// placeholder rule");
        commitRules();
    }

    public void stop() {
        LOG.debug("Stopping drools session and removing all rules.");
        final KieSession activeSession = session.getAndSet(null);
        if (activeSession != null) {
            activeSession.dispose();
        }
        if (currentReleaseId != null) {
            kieServices.getRepository().removeKieModule(currentReleaseId);
        }
    }

    @Override
    public synchronized boolean addRule(String ruleSource) {
        LOG.debug("Adding rule {}", ruleSource);
        liveRules.add(ruleSource);
        if (!commitRules()) {
            // adding rule failed, remove the ruleSource from our list of liveRules again.
            liveRules.remove(ruleSource);
            return false;
        }
        return true;
    }

    @Override
    public synchronized boolean addRulesFromFile(String rulesFile) {
        LOG.debug("Adding drools rules from file {}", rulesFile);
        try {
            final String rulesSource = Files.toString(new File(rulesFile), Charsets.UTF_8);
            return addRule(rulesSource);
        } catch (IOException e) {
            LOG.warn("Could not read drools source file. Not loading rules.", e);
        }
        return false;
    }

    @Override
    public int evaluateInSharedSession(Message message) {
        final KieSession kieSession = session.get();
        if (kieSession != null) {
            kieSession.insert(message);
            return kieSession.fireAllRules();
        }
        return 0;
    }

    @Override
    public RulesSession createPrivateSession() {
        final KieSession kieSession = kieContainer.newKieSession();
//        kieSession.addEventListener(new DebugRuleRuntimeEventListener());
//        kieSession.addEventListener(new DebugAgendaEventListener());
        kieSession.setGlobal("log", LOG);
        return new DroolsRulesSession(kieSession);
    }

    @Override
    public Object insertFact(Object fact) {
        return session.get().insert(fact);
    }

    @Override
    public boolean deleteFact(Object fact) {
        final FactHandle factHandle = session.get().getFactHandle(fact);
        if (factHandle == null) {
            return false;
        }
        session.get().delete(factHandle);
        return true;
    }

    private boolean commitRules() {
        final ReleaseId previousReleaseId = currentReleaseId;
        final ReleaseId newReleaseId = nextRulesPackageVersion();
        LOG.debug("Committing rules as version {}", newReleaseId);
        final boolean deployed = deployRules(newReleaseId);
        if (deployed && previousReleaseId != null) {
            kieServices.getRepository().removeKieModule(previousReleaseId);
        }
        return deployed;
    }

    private boolean deployRules(ReleaseId newReleaseId) {
        try {
            // add common header
            // TODO this will go wrong at some point, use the DRL6Parser to figure what to add,
            // and potentially also just modify the tree it generates to add the import and globals we want
            final String[] drls = new String[liveRules.size()];
            int i = 0;
            for (String drl : liveRules) {
                drls[i] = "package org.graylog2.rules\n" +
                        "import org.graylog2.plugin.*\n" +
                        "global org.slf4j.Logger log\n" +
                        "\n" + drl;
                i++;
            }

            createAndDeployJar(kieServices,
                               newReleaseId,
                               drls);
            if (kieContainer == null) {
                kieContainer = kieServices.newKieContainer(newReleaseId);
                final KieSession session = kieContainer.newKieSession();
                this.session.set(session);
                session.setGlobal("log", LOG);
            }
            kieContainer.updateToVersion(newReleaseId);
            return true;
        } catch (RulesCompilationException e) {
            LOG.warn("Unable to add rules due to compilation errors.", e);
            return false;
        }
    }

    private ReleaseId nextRulesPackageVersion() {
        currentReleaseId = kieServices.newReleaseId("org.graylog2", "dynamic-rules", Integer.toString(version++));
        return currentReleaseId;
    }

    private KieModule createAndDeployJar(KieServices ks,
                                                ReleaseId releaseId,
                                                String... drls) throws RulesCompilationException {
        byte[] jar = createKJar(ks, releaseId, null, drls);
        return deployJar(ks, jar);
    }

    private byte[] createKJar(KieServices ks,
                                     ReleaseId releaseId,
                                     String pom,
                                     String... drls) throws RulesCompilationException {
        KieFileSystem kfs = ks.newKieFileSystem();
        if (pom != null) {
            kfs.write("pom.xml", pom);
        } else {
            kfs.generateAndWritePomXML(releaseId);
        }
        for (int i = 0; i < drls.length; i++) {
            if (drls[i] != null) {
                kfs.write("src/main/resources/r" + i + ".drl", drls[i]);
            }
        }
        for (URL builtinRuleUrl : builtinRuleUrls) {
            final Path rulesPath = new File(builtinRuleUrl.getPath()).toPath();
            final String path = "src/main/resources/" + rulesPath.getFileName();
            final Resource resource = ResourceFactory
                    .newUrlResource(builtinRuleUrl)
                    .setSourcePath(path)
                    .setResourceType(ResourceType.DRL);
            kfs.write(resource);
        }

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new RulesCompilationException(kb.getResults().getMessages());
        }
        InternalKieModule kieModule = (InternalKieModule) ks.getRepository()
                .getKieModule(releaseId);
        byte[] jar = kieModule.getBytes();
        return jar;
    }

    private KieModule deployJar(KieServices ks, byte[] jar) {
        // Deploy jar into the repository
        Resource jarRes = ks.getResources().newByteArrayResource(jar);
        KieModule km = ks.getRepository().addKieModule(jarRes);
        return km;
    }
}
