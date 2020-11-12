// @flow strict
import UserNotification from 'util/UserNotification';
import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';

import type { WizardFormValues, AuthBackendMeta } from './BackendWizard/BackendWizardContext';

export default (payload: WizardSubmitPayload, formValues: WizardFormValues, serviceType: $PropertyType<AuthBackendMeta, 'serviceType'>, shouldUpdateGroupSync?: boolean = true) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const notifyOnSuccess = () => UserNotification.success('Authentication service was created successfully.', 'Success');
  const notifyOnError = (error) => UserNotification.error(`Creating authentication service failed with status: ${error}`, 'Error');

  return AuthenticationActions.create(payload).then((result) => {
    if (result.backend && formValues.synchronizeGroups && enterpriseGroupSyncPlugin && shouldUpdateGroupSync) {
      return enterpriseGroupSyncPlugin.actions.onDirectoryServiceBackendUpdate(false, formValues, result.backend.id, serviceType).then(notifyOnSuccess);
    }

    notifyOnSuccess();

    return result;
  }).catch((error) => {
    notifyOnError(error);
    throw error;
  });
};
