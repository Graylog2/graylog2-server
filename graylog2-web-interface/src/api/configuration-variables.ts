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

import UserNotification from 'util/UserNotification';

export const fetchAllConfigurationVariables = () => {
  const promise = SidecarConfigurationVariables.listConfigurationVariables();

  promise.catch((error: unknown) => {
    UserNotification.error(
      `Fetching configuration variables failed with status: ${error}`,
      'Could not retrieve configuration variables',
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
  const action = isCreate ? 'created' : 'updated';

  const promise = isCreate
    ? SidecarConfigurationVariables.createConfigurationVariable(request)
    : SidecarConfigurationVariables.updateConfigurationVariable(configurationVariable.id, request);

  promise.then(
    () => {
      UserNotification.success(`Configuration variable "${configurationVariable.name}" successfully ${action}`);
    },
    (error: { message?: string }) => {
      UserNotification.error(
        `Saving variable "${configurationVariable.name}" failed with status: ${error.message}`,
        'Could not save variable',
      );
    },
  );

  return promise;
};

export const deleteConfigurationVariable = (configurationVariable: { id: string; name: string }) => {
  const promise = SidecarConfigurationVariables.deleteConfigurationVariable(configurationVariable.id);

  promise.then(
    () => {
      UserNotification.success(`Configuration variable "${configurationVariable.name}" successfully deleted`);
    },
    (error: { message?: string }) => {
      UserNotification.error(
        `Deleting variable "${configurationVariable.name}" failed with status: ${error.message}`,
        'Could not delete variable',
      );
    },
  );

  return promise;
};

export const validateConfigurationVariable = (
  configurationVariable: Parameters<typeof SidecarConfigurationVariables.validateConfigurationVariable>[0],
) => {
  const payload = merge(
    {
      id: ' ',
      name: ' ',
      content: ' ',
    },
    configurationVariable,
  );

  const promise = SidecarConfigurationVariables.validateConfigurationVariable(payload);

  promise.catch((error: { message?: string }) => {
    UserNotification.error(
      `Validating variable "${configurationVariable.name}" failed with status: ${error.message}`,
      'Could not validate variable',
    );
  });

  return promise;
};

export const getConfigurationsForVariable = (configurationVariable: { id: string }) => {
  const promise = SidecarConfigurationVariables.getConfigurationVariablesConfigurations(configurationVariable.id);

  promise.catch((error: unknown) => {
    UserNotification.error(`Fetching configurations for this variable failed with status: ${error}`);
  });

  return promise;
};
