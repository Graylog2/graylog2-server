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
// @flow strict
// eslint-disable-next-line import/no-cycle
import { $PropertyType } from 'utility-types';

import FetchError from 'logic/errors/FetchError';

export const ReactErrorType = 'ReactError';
export const NotFoundErrorType = 'NotFoundError';
export const UnauthorizedErrorType = 'UnauthorizedError';
export const StreamPermissionErrorType = 'StreamPermissionError';

type ReactError = {
  error: Error,
  info: { componentStack: string },
  type: 'ReactError',
};
type NotFoundError = {
  error: FetchError,
  type: 'NotFoundError',
};
type UnauthorizedError = {
  error: FetchError,
  type: 'UnauthorizedError',
};
type StreamPermissionError = {
  error: FetchError,
  type: 'StreamPermissionError',
};

export type ReportedError = ReactError | NotFoundError | UnauthorizedError | StreamPermissionError;

export const createReactError = (error: $PropertyType<ReactError, 'error'>, info: $PropertyType<ReactError, 'info'>): ReactError => ({
  error,
  info,
  type: ReactErrorType,
});
export const createNotFoundError = (error: $PropertyType<NotFoundError, 'error'>): NotFoundError => ({
  error,
  type: NotFoundErrorType,
});
export const createUnauthorizedError = (error: $PropertyType<UnauthorizedError, 'error'>): UnauthorizedError => ({
  error,
  type: UnauthorizedErrorType,
});
export const createStreamPermissionError = (error: $PropertyType<StreamPermissionError, 'error'>): StreamPermissionError => ({
  error,
  type: StreamPermissionErrorType,
});

export const createFromFetchError = (error: FetchError) => {
  switch (error.status) {
    case 403:
      return error?.additional?.body?.type === 'MissingStreamPermission' ? createStreamPermissionError(error) : createUnauthorizedError(error);
    case 404:
      return createNotFoundError(error);
    default:
      throw Error(`Provided FetchError is not a valid ReportedError because status code ${error.status} is not supported`);
  }
};
