import com.google.common.collect.Lists;
import lib.security.ServerRestInterfaceRealm;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.mvc.Http;

/**
 *
 */
@SuppressWarnings("unused")
public class Global extends GlobalSettings {
	private static final Logger log = LoggerFactory.getLogger(Global.class);

	@Override
	public void onStart(Application app) {
		SimpleAccountRealm webInterfaceLocalRealm = new SimpleAccountRealm("local-accounts");
		webInterfaceLocalRealm.setCredentialsMatcher(new HashedCredentialsMatcher("MD5"));
		setupLocalUsers(webInterfaceLocalRealm, app);

		Realm serverRestInterfaceRealm = new ServerRestInterfaceRealm();
		final DefaultSecurityManager securityManager =
				new DefaultSecurityManager(
						Lists.newArrayList(serverRestInterfaceRealm, webInterfaceLocalRealm)
				);
		final Authenticator authenticator = securityManager.getAuthenticator();
		// TODO refactor this to somewhere it makes sense
		if (authenticator instanceof AbstractAuthenticator) {
			((AbstractAuthenticator) authenticator).setAuthenticationListeners(CollectionUtils.<AuthenticationListener>asList(new AuthenticationListener() {
				@Override
				public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
					final Http.Session session = Http.Context.current().session();
					session.put("username", token.getPrincipal().toString());
				}

				@Override
				public void onFailure(AuthenticationToken token, AuthenticationException ae) {
				}

				@Override
				public void onLogout(PrincipalCollection principals) {
					final Http.Session session = Http.Context.current().session();
					session.remove("username");
				}
			}));
		}
		SecurityUtils.setSecurityManager(securityManager);
	}

	private void setupLocalUsers(SimpleAccountRealm realm, Application app) {
		final Configuration config = app.configuration();
		if (config.getString("local-user.password-md5") == null) {
			log.warn("No password hash for local user {} set. " +
					"If you lose connection to the graylog2-server at {}, you will be unable to log in!",
					config.getString("local-user.name", "localadmin"), config.getString("graylog2-server"));
			return;
		}
		realm.addAccount(
				config.getString("local-user.name", "localadmin"),
				config.getString("local-user.password-md5"),
				"local-admin"
		);
	}
}
