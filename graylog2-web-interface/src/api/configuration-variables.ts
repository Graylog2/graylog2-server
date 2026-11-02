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
import merge from 'lodash/merge';

import { SidecarConfigurationVariables } from '@graylog/server-api';

import i18n from 'i18n';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

const SOURCE_URL = '/sidecar/configuration_variables';

export const fetchAllConfigurationVariables = () => {
  const promise = SidecarConfigurationVariables.listConfigurationVariables();

  promise.catch((error: unknown) => {
    UserNotification.error(
      i18n.t('notifications.fetch_failed', { ns: 'sidecars', error }),
      i18n.t('notifications.fetch_failed_title', { ns: 'sidecars' }),
    );
  });

  return promise;
};

export const saveConfigurationVariable = (configurationVariable: {
  id: string;
  name: string;
  description: string;
  content: string;
}) => {
  const request = {
    id: configurationVariable.id,
    name: configurationVariable.name,
    description: configurationVariable.description,
    content: configurationVariable.content,
  };

  const isCreate = configurationVariable.id === '';

  const promise = isCreate
    ? SidecarConfigurationVariables.createConfigurationVariable(request)
    : SidecarConfigurationVariables.updateConfigurationVariable(configurationVariable.id, request);

  promise.then(
    () => {
      UserNotification.success(
        i18n.t(isCreate ? 'notifications.save_success_created' : 'notifications.save_success_updated', {
          ns: 'sidecars',
          name: configurationVariable.name,
        }),
      );
    },
    (error: { message?: string }) => {
      UserNotification.error(
        i18n.t('notifications.save_failed', { ns: 'sidecars', name: configurationVariable.name, error: error.message }),
        i18n.t('notifications.save_failed_title', { ns: 'sidecars' }),
      );
    },
  );

  return promise;
};

export const deleteConfigurationVariable = (configurationVariable: { id: string; name: string }) => {
  const promise = SidecarConfigurationVariables.deleteConfigurationVariable(configurationVariable.id);

  promise.then(
    () => {
      UserNotification.success(
        i18n.t('notifications.delete_success', { ns: 'sidecars', name: configurationVariable.name }),
      );
    },
    (error: { message?: string }) => {
      UserNotification.error(
        i18n.t('notifications.delete_failed', {
          ns: 'sidecars',
          name: configurationVariable.name,
          error: error.message,
        }),
        i18n.t('notifications.delete_failed_title', { ns: 'sidecars' }),
      );
    },
  );

  return promise;
};

export const validateConfigurationVariable = (configurationVariable: { name?: string; [key: string]: unknown }) => {
  const payload: Record<string, unknown> = {
    id: ' ',
    name: ' ',
    content: ' ',
  };

  merge(payload, configurationVariable);

  const promise = fetch('POST', qualifyUrl(`${SOURCE_URL}/validate`), payload);

  promise.catch((error: { message?: string }) => {
    UserNotification.error(
      i18n.t('notifications.validate_failed', {
        ns: 'sidecars',
        name: configurationVariable.name,
        error: error.message,
      }),
      i18n.t('notifications.validate_failed_title', { ns: 'sidecars' }),
    );
  });

  return promise;
};

export const getConfigurationsForVariable = (configurationVariable: { id: string }) => {
  const promise = SidecarConfigurationVariables.getConfigurationVariablesConfigurations(configurationVariable.id);

  promise.catch((error: unknown) => {
    UserNotification.error(i18n.t('notifications.configurations_fetch_failed', { ns: 'sidecars', error }));
  });

  return promise;
};
