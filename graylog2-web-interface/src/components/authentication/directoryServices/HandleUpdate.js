// @flow strict
import UserNotification from 'util/UserNotification';
import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';

import type { WizardFormValues } from './BackendWizard/BackendWizardContext';

export default (payload: WizardSubmitPayload, formValues: WizardFormValues, backendId: string, backendGroupSyncIsActive: boolean, serviceType: string, shouldUpdateGroupSync: ?boolean = true) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const notifyOnSuccess = () => UserNotification.success('Authentication service was updated successfully.', 'Success');
  const notifyOnError = (error) => UserNotification.error(`Updating authentication service failed with status: ${error}`, 'Error');

  return AuthenticationActions.update(backendId, {
    ...payload,
    id: backendId,
  }).then((result) => {
    if (enterpriseGroupSyncPlugin && shouldUpdateGroupSync) {
      return enterpriseGroupSyncPlugin.actions.onDirectoryServiceBackendUpdate(backendGroupSyncIsActive, formValues, backendId, serviceType).then(notifyOnSuccess);
    }

    notifyOnSuccess();

    return result;
  }).catch((error) => {
    notifyOnError(error);
    throw error;
  });
};
