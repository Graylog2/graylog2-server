// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import type { Step } from 'components/common/Wizard';
import { singleton } from 'views/logic/singleton';

export type WizardFormValues = {
  defaultRoles?: string,
  groupSearchBase?: string,
  groupSearchPattern?: string,
  serverUrlHost?: string,
  serverUrlPort?: string | number,
  systemUserDn?: string,
  systemUserPassword?: string,
  synchronizeGroups?: boolean,
  teamDefaultRoles?: string,
  teamNameAttribute?: string,
  teamUniqueIdAttribute?: string,
  teamSelectionType?: 'all' | 'include' | 'exclude',
  teamSelection?: Immutable.Set<string>,
  transportSecurity?: 'tls' | 'start_tls' | 'none',
  userFullNameAttribute?: string,
  userNameAttribute?: string,
  userSearchBase?: string,
  userSearchPattern?: string,
  verifyCertificates?: boolean,
};
export type AuthBackendMeta = {
  backendId?: string, // only needed when editing an auth service
  backendHasPassword?: boolean, // only needed when editing an auth service
  serviceType: string,
  serviceTitle: string,
  urlScheme: string,
};
export type WizardStepsState = {
  activeStepKey: $PropertyType<Step, 'key'>,
  formValues: WizardFormValues,
  invalidStepKeys: Array<string>,
  authBackendMeta: AuthBackendMeta,
  loadGroupsResult: ?{
    success: boolean,
    message: string,
    errors: Immutable.List<string>,
    result: {
      groups: Immutable.List<any>,
    },
  },
};

export type BackendWizardType = WizardStepsState & {
  setStepsState: (BackendWizardType) => void,
};

const initialState = {
  activeStepKey: '',
  authBackendMeta: {},
  formValues: {},
  invalidStepKeys: [],
  loadGroupsResult: undefined,
  setStepsState: () => {},
};

const BackendWizardContext = React.createContext<BackendWizardType>(initialState);
export default singleton('contexts.authentication.ldap.wizard', () => BackendWizardContext);
