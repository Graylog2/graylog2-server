import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import LdapConfigParser from 'logic/authentication/ldap/LdapConfigParser';

import BackendCreateLDAP from './ldap/BackendCreate';
import BackendEditLDAP from './ldap/BackendEdit';
import BackendConfigDetails from './ldap/BackendConfigDetails';
import BackendCreateAD from './activeDirectory/BackendCreate';
import BackendEditAD from './activeDirectory/BackendEdit';

PluginStore.register(new PluginManifest({}, {
  'authentication.services': [
    {
      name: 'ldap',
      displayName: 'LDAP',
      createComponent: BackendCreateLDAP,
      editComponent: BackendEditLDAP,
      configDetailsComponent: BackendConfigDetails,
      configToJson: LdapConfigParser.toJson,
      configFromJson: LdapConfigParser.fromJson,
    },
    {
      name: 'active-directory',
      displayName: 'Active Directory',
      createComponent: BackendCreateAD,
      editComponent: BackendEditAD,
      configDetailsComponent: BackendConfigDetails,
      configToJson: LdapConfigParser.toJson,
      configFromJson: LdapConfigParser.fromJson,
    },
  ],
}));
