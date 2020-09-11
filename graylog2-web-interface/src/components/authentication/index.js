import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import MongoDbPasswordConfig from './legacy/MongoDbPasswordConfig';
import MongoDbSessionConfig from './legacy/MongoDbSessionConfig';
import LegacyLdapConfig from './legacy/LegacyLdapConfig';
import RootUserConfig from './legacy/RootUserConfig';
import AccessTokenConfig from './legacy/AccessTokenConfig';
import BackendCreateLDAP from './ldap/BackendCreate';
import BackendEditLDAP from './ldap/BackendEdit';
import BackendSettingsLDAP from './BackendDetails/BackendSettings';

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
  authenticationServices: [
    {
      name: 'ldap',
      displayName: 'LDAP',
      createComponent: BackendCreateLDAP,
      editComponent: BackendEditLDAP,
      detailsComponent: BackendSettingsLDAP,
      configMapJson: ({
        default_groups: 'defaultGroups',
        display_name_attribute: 'displayNameAttribute',
        encrypted_system_password: 'encryptedSystemPassword',
        server_uri: 'serverUri',
        system_username: 'systemUsername',
        trust_all_certificates: 'trustAllCertificates',
        user_search_base: 'userSearchBase',
        user_search_pattern: 'userSearchPattern',
        use_start_tls: 'useStartTls',
        use_ssl: 'useSsl',
      }),
    },
    {
      name: 'active-directory',
      displayName: 'Active Directory',
      createComponent: BackendCreateLDAP,
      // detailsComponent: BackendSettingsAD,
    },
  ],
}));
