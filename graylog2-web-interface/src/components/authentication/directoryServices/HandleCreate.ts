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
import i18n from 'i18n';
import UserNotification from 'util/UserNotification';
import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import { createAuthBackend } from 'hooks/useAuthentication';

import type { WizardFormValues, AuthBackendMeta } from './BackendWizard/BackendWizardContext';

export default (
  payload: WizardSubmitPayload,
  formValues: WizardFormValues,
  serviceType: AuthBackendMeta['serviceType'],
  shouldUpdateGroupSync: boolean | undefined = true,
) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const notifyOnSuccess = () =>
    UserNotification.success(
      i18n.t('notifications.create_success', { ns: 'authentication' }),
      i18n.t('notifications.success_title', { ns: 'authentication' }),
    );
  const notifyOnError = (error) =>
    UserNotification.error(
      i18n.t('notifications.create_failed', { ns: 'authentication', error }),
      i18n.t('notifications.error_title', { ns: 'authentication' }),
    );

  return createAuthBackend(payload)
    .then((result) => {
      if (result.backend && formValues.synchronizeGroups && enterpriseGroupSyncPlugin && shouldUpdateGroupSync) {
        return enterpriseGroupSyncPlugin.actions
          .onDirectoryServiceBackendUpdate(false, formValues, result.backend.id, serviceType)
          .then(notifyOnSuccess)
          .then(() => result);
      }

      notifyOnSuccess();

      return result;
    })
    .catch((error) => {
      notifyOnError(error);
      throw error;
    });
};
