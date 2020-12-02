/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { $PropertyType } from 'utility-types';

import UserNotification from 'util/UserNotification';
import { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';

import { WizardFormValues, AuthBackendMeta } from './BackendWizard/BackendWizardContext';

export default (payload: WizardSubmitPayload, formValues: WizardFormValues, serviceType: $PropertyType<AuthBackendMeta, 'serviceType'>, shouldUpdateGroupSync: boolean | undefined = true) => {
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
