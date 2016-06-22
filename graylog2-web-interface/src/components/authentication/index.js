import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import MongoDbPasswordConfig from './MongoDbPasswordConfig';
import MongoDbSessionConfig from './MongoDbSessionConfig';
import LegacyLdapConfig from './LegacyLdapConfig';
import RootUserConfig from './RootUserConfig';
import AccessTokenConfig from './AccessTokenConfig';

PluginStore.register(new PluginManifest({}, {
  authenticatorConfigurations: [
    {
      name: 'mongodb-password',
      displayName: 'Passwords',
      component: MongoDbPasswordConfig,
    },
    {
      name: 'mongodb-session',
      displayName: 'Sessions',
      component: MongoDbSessionConfig,
    },
    {
      name: 'legacy-ldap',
      displayName: 'LDAP/Active Directory',
      component: LegacyLdapConfig,
    },
    {
      name: 'root-user',
      displayName: 'Admin user',
      component: RootUserConfig,
    },
    {
      name: 'access-token',
      displayName: 'Access Tokens',
      component: AccessTokenConfig,
    },
  ],
}));
