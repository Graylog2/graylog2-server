package lib.security;

import lib.APIException;
import lib.Api;
import models.api.responses.system.AuthenticationResponse;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		final AuthenticationResponse response;
		try {
			// TODO implement POST and actually send stuff. Should the password be hashed here already?
			response = Api.get("/system/auth", AuthenticationResponse.class);
			log.debug("Trying to log in {} via REST", token.getPrincipal());
			if (!response.isAuthorized) {
				throw new UnauthorizedException();
			}
		} catch (IOException e) {
			throw new AuthenticationException("Unable to communicate with graylog2-server backend", e);
		} catch (APIException e) {
			throw new AuthenticationException("Unable to communicate with graylog2-server backend", e);
		}
		return new SimpleAuthenticationInfo(response.username, token.getCredentials(), "rest-interface");
	}
}
