import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import MongoDbPasswordConfig from './MongoDbPasswordConfig';
import MongoDbSessionConfig from './MongoDbSessionConfig';
import LegacyLdapConfig from './LegacyLdapConfig';
import RootUserConfig from './RootUserConfig';
import AccessTokenConfig from './AccessTokenConfig';
import ServiceCreateLDAP from './ldap/ServiceCreate';
import ServiceEditLDAP from './ldap/ServiceEdit';
import ServiceSettingsLDAP from './ldap/ServiceSettings';

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
      createComponent: ServiceCreateLDAP,
      editComponent: ServiceEditLDAP,
      detailsComponent: ServiceSettingsLDAP,
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
      createComponent: ServiceCreateLDAP,
      // detailsComponent: ServiceSettingsAD,
    },
  ],
}));
