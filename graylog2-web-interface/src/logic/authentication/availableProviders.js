// @flow strict
export const availableProviders = {
  ldap: 'LDAP',
  activeDirectory: 'Active Directory',
};

export const availableProvidersOptions = Object.entries(availableProviders).map(([value, label]) => ({ value, label }));
