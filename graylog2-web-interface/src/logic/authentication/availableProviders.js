// @flow strict
import Routes from 'routing/Routes';

export const availableProviders = {
  ldap: { name: 'LDAP', route: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE_LDAP },
  activeDirectory: { name: 'Active Directory', route: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE_AD },
};

export const availableProvidersOptions: Array<{value: string, label: string}> = Object.keys(availableProviders)
  .map((value) => ({ value, label: availableProviders[value].name }));
