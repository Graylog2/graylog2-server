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
      description: 'Graylog managed account passwords (from MongoDB)',
      canBeDisabled: true,
      component: MongoDbPasswordConfig,
    },
    {
      name: 'mongodb-session',
      displayName: 'Sessions',
      description: 'Established session authenticator',
      canBeDisabled: false,
      component: MongoDbSessionConfig,
    },
    {
      name: 'legacy-ldap',
      displayName: 'LDAP/Active Directory',
      description: 'Authenticates against external system and creates accounts in Graylog',
      canBeDisabled: true,
      component: LegacyLdapConfig,
    },
    {
      name: 'root-user',
      displayName: 'Admin user',
      description: 'Static account configured in the server configuration file',
      canBeDisabled: false,
      component: RootUserConfig,
    },
    {
      name: 'access-token',
      displayName: 'API Tokens',
      description: 'Per user tokens which do not establish sessions',
      canBeDisabled: true,
      component: AccessTokenConfig,
    },
  ],
}));
