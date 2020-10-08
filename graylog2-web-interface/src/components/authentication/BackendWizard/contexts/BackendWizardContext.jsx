// @flow strict
import * as React from 'react';

import type { Step } from 'components/common/Wizard';
import { singleton } from 'views/logic/singleton';

export type WizardFormValues = {
  defaultRoles?: string,
  groupSearchBase?: string,
  groupSearchPattern?: string,
  serverHost?: string,
  serverPort?: string | number,
  systemUserDn?: string,
  systemUserPassword?: string,
  teamDefaultRoles?: string,
  teamNameAttribute?: string,
  teamUniqueIdAttribute?: string,
  transportSecurity?: ('tls' | 'start_tls' | 'none'),
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
};
export type WizardStepsState = {
  activeStepKey: $PropertyType<Step, 'key'>,
  formValues: WizardFormValues,
  invalidStepKeys: Array<string>,
  authBackendMeta: AuthBackendMeta,
};

export type BackendWizardType = WizardStepsState & {
  setStepsState: (BackendWizardType) => void,
};

const initialState = {
  setStepsState: () => {},
  activeStepKey: '',
  formValues: {},
  authBackendMeta: {},
  invalidStepKeys: [],
};

const BackendWizardContext = React.createContext<BackendWizardType>(initialState);
export default singleton('contexts.authentication.ldap.wizard', () => BackendWizardContext);
