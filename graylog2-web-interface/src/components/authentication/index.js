import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import MongoDbPasswordConfig from './legacy/MongoDbPasswordConfig';
import MongoDbSessionConfig from './legacy/MongoDbSessionConfig';
import RootUserConfig from './legacy/RootUserConfig';
import AccessTokenConfig from './legacy/AccessTokenConfig';
import BackendCreateLDAP from './ldap/BackendCreate';
import BackendEditLDAP from './ldap/BackendEdit';
import BackendSettings from './BackendDetails/BackendSettings';
import BackendCreateAD from './activeDirectory/BackendCreate';
import BackendEditAD from './activeDirectory/BackendEdit';

PluginStore.register(new PluginManifest({}, {
  'authentication.services': [
    {
      name: 'ldap',
      displayName: 'LDAP',
      createComponent: BackendCreateLDAP,
      editComponent: BackendEditLDAP,
      detailsComponent: BackendSettings,
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
      createComponent: BackendCreateAD,
      editComponent: BackendEditAD,
      detailsComponent: BackendSettings,
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
  ],
}));
