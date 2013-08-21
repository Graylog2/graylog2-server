package lib.security;

import lib.APIException;
import lib.Api;
import models.User;
import models.api.responses.system.AuthenticationResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.Cache;

import java.io.IOException;

/**
 * Shiro Realm implementation that uses a Graylog2-server as the source of the subject's information.
 */
public class ServerRestInterfaceRealm extends AuthorizingRealm {
	private static final Logger log = LoggerFactory.getLogger(ServerRestInterfaceRealm.class);

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		// currently we don't have any authorization information yet :(
		return null;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
		final AuthenticationResponse response;
		if (!(authToken instanceof UsernamePasswordToken)) {
			throw new IllegalStateException("Expected UsernamePasswordToken");
		}
		UsernamePasswordToken token = (UsernamePasswordToken) authToken;
		try {
			// TODO implement POST and actually send stuff. Should the password be hashed here already?
			final SimpleHash sha1 = new SimpleHash("SHA1", token.getPassword());    // , Play.application().configuration().getString("secret")
			// TODO string concat in url sucks, use messageformat or something that actually encodes, too
			response = Api.get("/users/"+token.getUsername(), AuthenticationResponse.class, token.getUsername(), sha1.toString());
			log.debug("Trying to log in {} via REST", token.getUsername());
			final User user = new User(response.id, response.username, "", response.fullName);
			// TODO AAAAAAAAARG
			Cache.set(user.getName(), user);
			new Subject.Builder(SecurityUtils.getSecurityManager())
					.authenticated(true)
					.buildSubject();
		} catch (IOException e) {
			throw new AuthenticationException("Unable to communicate with graylog2-server backend", e);
		} catch (APIException e) {
			throw new AuthenticationException("Server responded with non-200 code", e);
		}
		return new SimpleAuthenticationInfo(response.username, authToken.getCredentials(), "rest-interface");
	}
}
