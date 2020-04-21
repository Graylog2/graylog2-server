// @flow strict
// eslint-disable-next-line import/no-cycle
import { FetchError } from 'logic/rest/FetchProvider';

export const ReactErrorType = 'ReactError';
export const NotFoundErrorType = 'NotFoundError';
export const UnauthorizedErrorType = 'UnauthorizedError';
export const StreamPermissionErrorType = 'StreamPermissionError';

type ReactError = {
  error: Error,
  info: { componentStack: string },
  type: 'ReactError',
};
type UnauthorizedError = {
  error: FetchError,
  type: 'UnauthorizedError',
};
type StreamPermissionError = {
  error: FetchError,
  type: 'StreamPermissionError',
};

export type ReportedError = ReactError | UnauthorizedError;

export const createReactError = (error: $PropertyType<ReactError, 'error'>, info: $PropertyType<ReactError, 'info'>): ReactError => ({
  error,
  info,
  type: ReactErrorType,
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
    default:
      throw Error(`Provided FetchError is not a valid ReportedError because status code ${error.status} is not supported`);
  }
};
