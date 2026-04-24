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

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

const SOURCE_URL = '/sidecar/configuration_variables';

export const fetchAllConfigurationVariables = () => {
  const promise = fetch('GET', qualifyUrl(SOURCE_URL));

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

  let url = qualifyUrl(SOURCE_URL);
  let method: 'POST' | 'PUT';
  let action: string;

  if (configurationVariable.id === '') {
    method = 'POST';
    action = 'created';
  } else {
    url += `/${configurationVariable.id}`;
    method = 'PUT';
    action = 'updated';
  }

  const promise = fetch(method, url, request);

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
  const url = qualifyUrl(`${SOURCE_URL}/${configurationVariable.id}`);
  const promise = fetch('DELETE', url);

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
      `Validating variable "${configurationVariable.name}" failed with status: ${error.message}`,
      'Could not validate variable',
    );
  });

  return promise;
};

export const getConfigurationsForVariable = (configurationVariable: { id: string }) => {
  const url = qualifyUrl(`${SOURCE_URL}/${configurationVariable.id}/configurations`);
  const promise = fetch('GET', url);

  promise.catch((error: unknown) => {
    UserNotification.error(`Fetching configurations for this variable failed with status: ${error}`);
  });

  return promise;
};
