// @flow strict
import * as React from 'react';

import type { Step } from 'components/common/Wizard';
import { singleton } from 'views/logic/singleton';

export type WizardFormValues = {
  defaultRoles?: string,
  groupNameAttribute?: string,
  groupSearchBase?: string,
  groupSearchPattern?: string,
  serverUrlHost?: string,
  serverUrlPort?: string | number,
  systemUserDn?: string,
  systemUserPassword?: string,
  transportSecurity?: ('tls' | 'start_tls' | ''),
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
