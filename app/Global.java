import com.google.common.collect.Lists;
import lib.security.ServerRestInterfaceRealm;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import play.Application;
import play.GlobalSettings;

/**
 *
 */
public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		SimpleAccountRealm webInterfaceLocalRealm = new SimpleAccountRealm("local-accounts");
		webInterfaceLocalRealm.addAccount("admin", "123123123"); // TODO LOL
		Realm serverRestInterfaceRealm = new ServerRestInterfaceRealm();
		final DefaultSecurityManager securityManager = new DefaultSecurityManager(Lists.newArrayList(serverRestInterfaceRealm, webInterfaceLocalRealm));
		SecurityUtils.setSecurityManager(securityManager);
	}
}
