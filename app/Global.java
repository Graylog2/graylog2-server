/*
 * Copyright 2013 TORCH UG
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
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import lib.ApiClient;
import lib.ServerNodesRefreshService;
import lib.security.PlayAuthenticationListener;
import lib.security.RedirectAuthenticator;
import lib.security.RethrowingFirstSuccessfulStrategy;
import lib.security.ServerRestInterfaceRealm;
import models.LocalAdminUser;
import models.ModelFactoryModule;
import models.Node;
import models.UserService;
import models.api.responses.NodeSummaryResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Application;
import play.Configuration;
import play.GlobalSettings;

import java.util.List;

/**
 *
 */
@SuppressWarnings("unused")
public class Global extends GlobalSettings {
	private static final Logger log = LoggerFactory.getLogger(Global.class);
    private Injector injector;

    @Override
	public void onStart(Application app) {

        final String appSecret = app.configuration().getString("application.secret");
        if (appSecret == null || appSecret.isEmpty()) {
            log.error("Please configure application.secret in your application.conf");
            throw new IllegalStateException("No application.secret configured.");
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
        final Node[] initialNodes = new Node[uris.length];
        int i = 0;
        for (String uri : uris) {
            final NodeSummaryResponse r = new NodeSummaryResponse();
            r.transportAddress =  uri;
            initialNodes[i++] = new Node(r);  // TODO DI this is wrong, can we use the factory already here?
        }

        List<Module> modules = Lists.newArrayList();
        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Node[].class).annotatedWith(Names.named("Initial Nodes")).toInstance(initialNodes);
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

        // temporarily disabled for preview to prevent confusion.
//        LocalAdminUserRealm localAdminRealm = new LocalAdminUserRealm("local-accounts");
//        localAdminRealm.setCredentialsMatcher(new HashedCredentialsMatcher("SHA2"));
//        setupLocalUser(api, localAdminRealm, app);

        Realm serverRestInterfaceRealm = injector.getInstance(ServerRestInterfaceRealm.class);
        final DefaultSecurityManager securityManager =
                new DefaultSecurityManager(
                        Lists.newArrayList(serverRestInterfaceRealm)
                );
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

    @Override
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        return injector.getInstance(controllerClass);
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
