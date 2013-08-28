import com.google.common.collect.Lists;
import lib.security.LocalAdminUserRealm;
import lib.security.PlayAuthenticationListener;
import lib.security.RethrowingFirstSuccessfulStrategy;
import lib.security.ServerRestInterfaceRealm;
import models.User;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Application;
import play.Configuration;
import play.GlobalSettings;

import java.io.File;

/**
 *
 */
@SuppressWarnings("unused")
public class Global extends GlobalSettings {
	private static final Logger log = LoggerFactory.getLogger(Global.class);

    @Override
    public Configuration onLoadConfig(Configuration configuration, File file, ClassLoader classLoader) {
        // TODO implement dynamic work area to override configuration settings
        return super.onLoadConfig(configuration, file, classLoader);
    }

    @Override
	public void onStart(Application app) {
        LocalAdminUserRealm localAdminRealm = new LocalAdminUserRealm("local-accounts");
		localAdminRealm.setCredentialsMatcher(new HashedCredentialsMatcher("SHA1"));
		setupLocalUser(localAdminRealm, app);

		Realm serverRestInterfaceRealm = new ServerRestInterfaceRealm();
		final DefaultSecurityManager securityManager =
				new DefaultSecurityManager(
						Lists.newArrayList(localAdminRealm, serverRestInterfaceRealm)
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

	private void setupLocalUser(SimpleAccountRealm realm, Application app) {
		final Configuration config = app.configuration();
        final String username = config.getString("local-user.name", "localadmin");
        final String passwordHash = config.getString("local-user.password-sha1");
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
        User.LocalAdminUser.createSharedInstance(username, passwordHash);
    }

}
