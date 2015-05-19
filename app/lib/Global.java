/*
 * Copyright 2013-2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package lib;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lib.security.PlayAuthenticationListener;
import lib.security.RedirectAuthenticator;
import lib.security.RethrowingFirstSuccessfulStrategy;
import lib.security.ServerRestInterfaceRealm;
import models.LocalAdminUser;
import models.ModelFactoryModule;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.graylog2.logback.appender.AccessLog;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.DateTools;
import org.graylog2.restclient.lib.Graylog2MasterUnavailableException;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.lib.ServerNodesRefreshService;
import org.graylog2.restclient.lib.Tools;
import org.graylog2.restclient.lib.Version;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.SessionService;
import org.graylog2.restclient.models.UserService;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.api.mvc.EssentialFilter;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static play.mvc.Results.internalServerError;

@SuppressWarnings("unused")
public class Global extends GlobalSettings {
    private static final Logger log = LoggerFactory.getLogger(Global.class);

    private static Injector injector;

    private boolean gelfAccessLog = false;

    /**
     * Retrieve the application's global Guice injector.
     * <p/>
     * Unfortunately there seems to be no supported way to store custom objects in the application,
     * thus we need to make this accessor static. However, running more than one Play application in
     * the same JVM won't work anyway, so we are on the safe side here.
     *
     * @return the Guice injector for this application.
     */
    public static Injector getInjector() {
        return injector;
    }

    @Override
    public void onStart(Application app) {
        log.info("Graylog web interface version {} starting up.", Version.VERSION);

        final String appSecret = app.configuration().getString("application.secret");
        if (appSecret == null || appSecret.isEmpty()) {
            log.error("Please configure application.secret in your conf/graylog-web-interface.conf");
            throw new IllegalStateException("No application.secret configured.");
        }
        if (appSecret.length() < 16) {
            log.error("Please configure application.secret in your conf/graylog-web-interface.conf to be longer than 16 characters. Suggested is using pwgen -N 1 -s 96 or similar");
            throw new IllegalStateException("application.secret is too short, use at least 16 characters! Suggested is to use pwgen -N 1 -s 96 or similar");
        }

        final String graylog2ServerUris = app.configuration().getString("graylog2-server.uris", "");
        if (graylog2ServerUris.isEmpty()) {
            log.error("graylog2-server.uris is not set!");
            throw new IllegalStateException("graylog2-server.uris is empty");
        }
        final String[] uris = graylog2ServerUris.split(",");
        if (uris.length == 0) {
            log.error("graylog2-server.uris is empty!");
            throw new IllegalStateException("graylog2-server.uris is empty");
        }
        final URI[] initialNodes = new URI[uris.length];
        int i = 0;
        for (String uri : uris) {
            try {
                initialNodes[i++] = new URI(uri);
            } catch (URISyntaxException e) {
                log.error("Invalid URI in 'graylog2-server.uris': " + uri, e);
            }
        }
        final String timezone = app.configuration().getString("timezone", "");
        if (!timezone.isEmpty()) {
            try {
                DateTools.setApplicationTimeZone(DateTimeZone.forID(timezone));
            } catch (IllegalArgumentException e) {
                log.error("Invalid timezone {} specified!", timezone);
                throw new IllegalStateException(e);
            }
        }
        log.info("Using application default timezone {}", DateTools.getApplicationTimeZone());

        // Dirty hack to disable the play2-graylog2 AccessLog if the plugin isn't there
        gelfAccessLog = app.configuration().getBoolean("graylog2.appender.send-access-log", false);

        final ObjectMapper objectMapper = buildObjectMapper();
        Json.setObjectMapper(objectMapper);

        final List<Module> modules = Lists.newArrayList();
        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(URI[].class).annotatedWith(Names.named("Initial Nodes")).toInstance(initialNodes);
                bind(Long.class).annotatedWith(Names.named("Default Timeout"))
                        .toInstance(org.graylog2.restclient.lib.Configuration.apiTimeout("DEFAULT"));
                bind(ObjectMapper.class).toInstance(objectMapper);
            }
        });
        modules.add(new ModelFactoryModule());
        injector = Guice.createInjector(modules);

        // start the services that need starting
        final ApiClient api = injector.getInstance(ApiClient.class);
        api.start();
        injector.getInstance(ServerNodesRefreshService.class).start();
        // TODO replace with custom AuthenticatedAction filter
        RedirectAuthenticator.userService = injector.getInstance(UserService.class);
        RedirectAuthenticator.sessionService = injector.getInstance(SessionService.class);

        // temporarily disabled for preview to prevent confusion.
//        LocalAdminUserRealm localAdminRealm = new LocalAdminUserRealm("local-accounts");
//        localAdminRealm.setCredentialsMatcher(new HashedCredentialsMatcher("SHA2"));
//        setupLocalUser(api, localAdminRealm, app);

        Realm serverRestInterfaceRealm = injector.getInstance(ServerRestInterfaceRealm.class);
        final DefaultSecurityManager securityManager =
                new DefaultSecurityManager(
                        Lists.newArrayList(serverRestInterfaceRealm)
                );
        // disable storing sessions (TODO we might want to write a session store bridge to play's session cookie)
        final DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        final DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        securityManager.setSubjectDAO(subjectDAO);

        final Authenticator authenticator = securityManager.getAuthenticator();
        if (authenticator instanceof ModularRealmAuthenticator) {
            ModularRealmAuthenticator a = (ModularRealmAuthenticator) authenticator;
            a.setAuthenticationStrategy(new RethrowingFirstSuccessfulStrategy());
            a.setAuthenticationListeners(
                    Lists.<AuthenticationListener>newArrayList(new PlayAuthenticationListener())
            );
        }
        SecurityUtils.setSecurityManager(securityManager);

    }

    private ObjectMapper buildObjectMapper() {
        return new ObjectMapper()
                .registerModules(new GuavaModule(), new JodaModule())
                .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
                .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public void onStop(Application app) {
        injector.getInstance(ApiClient.class).stop();
        injector.getInstance(ServerNodesRefreshService.class).stop();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends EssentialFilter> Class<T>[] filters() {
        final List<Class<T>> filters = Lists.newArrayList();
        filters.add((Class<T>) NoCacheHeader.class);

        if (gelfAccessLog) {
            filters.add((Class<T>) AccessLog.class);
        }

        final Class<T>[] result = new Class[filters.size()];
        return filters.toArray(result);
    }

    @Override
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        return injector.getInstance(controllerClass);
    }

    @Override
    public F.Promise<Result> onError(Http.RequestHeader request, Throwable t) {
        if (t.getCause() instanceof Graylog2MasterUnavailableException) {
            final ServerNodes serverNodes = injector.getInstance(ServerNodes.class);
            final List<Node> configuredNodes = serverNodes.getConfiguredNodes();
            final List<Node> nodesEverConnectedTo = serverNodes.all(true);

            return F.Promise.<Result>pure(internalServerError(
                            views.html.disconnected.no_master.render(Http.Context.current(), configuredNodes, nodesEverConnectedTo, serverNodes))
            );
        }
        return super.onError(request, t);
    }

    @Override
    public Configuration onLoadConfig(Configuration configuration, File file, ClassLoader classLoader) {
        boolean isTest = false;
        if (System.getProperty("skip.config.check", "false").equals("true")) {
            log.info("In test mode, not performing config file checks.");
            isTest = true;
        }
        final String configOverrideLocation = Tools.firstNonNull("",
                System.getProperty("config.file"),
                System.getProperty("config.url"),
                System.getProperty("config.resource"));
        if (!configOverrideLocation.isEmpty()) {
            log.warn("Using configuration from overridden location at {}", configOverrideLocation);
            return configuration;
        }

        final File configFile = new File(file, "conf/graylog-web-interface.conf");
        if (!isTest) {
            if (!configFile.exists()) {
                log.error("Your configuration should be at {} but does not exist, cannot continue without it.", configFile.getAbsoluteFile());
                throw new IllegalStateException("Missing configuration file " + configFile.getAbsolutePath());
            } else if (!configFile.canRead()) {
                log.error("Your configuration at {} is not readable, cannot continue without it.", configFile.getAbsoluteFile());
                throw new IllegalStateException("Unreadable configuration file " + configFile.getAbsolutePath());
            }
        }
        final Config config = ConfigFactory.parseFileAnySyntax(configFile);
        if (config.isEmpty() && !isTest) {
            log.error("Your configuration file at {} is empty, cannot continue without content.", configFile.getAbsoluteFile());
            throw new IllegalStateException("Empty configuration file " + configFile.getAbsolutePath());
        /*
         *
         * This is merging the standard bundled application.conf with our graylog-web-interface.conf.
         * The application.conf must always be empty when packaged so there is nothing hidden from the user.
         * We are merging, because the Configuration object already contains some information the web-interface needs.
         *
         */
        }
        return new Configuration(
                config.withFallback(configuration.getWrappedConfiguration().underlying())
        );
    }

    private void setupLocalUser(ApiClient api, SimpleAccountRealm realm, Application app) {
        final Configuration config = app.configuration();
        final String username = config.getString("local-user.name", "localadmin");
        final String passwordHash = config.getString("local-user.password-sha2");
        if (passwordHash == null) {
            log.warn("No password hash for local user {} set. " +
                            "If you lose connection to the graylog2-server at {}, you will be unable to log in!",
                    username, config.getString("graylog2-server"));
            return;
        }
        realm.addAccount(
                username,
                passwordHash,
                "local-admin"
        );
        LocalAdminUser.createSharedInstance(api, username, passwordHash);
    }

}
