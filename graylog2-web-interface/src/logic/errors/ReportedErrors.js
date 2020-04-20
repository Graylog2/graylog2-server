// @flow strict
// eslint-disable-next-line import/no-cycle
import { FetchError } from 'logic/rest/FetchProvider';

export const ReactErrorType = 'ReactError';
export const UnauthoriedErrorType = 'UnauthorizedError';

type ReactError = {
  error: Error,
  info: { componentStack: string },
  type: 'ReactError'
}

type UnauthorizedError = {
  error: FetchError,
  type: 'UnauthorizedError'
}

export type ReportedError = ReactError | UnauthorizedError

export const createReactError = (error: $PropertyType<ReactError, 'error'>, info: $PropertyType<ReactError, 'info'>): ReactError => ({
  error,
  info,
  type: ReactErrorType,
});

export const createUnauthorizedError = (error: $PropertyType<UnauthorizedError, 'error'>): UnauthorizedError => ({
  error,
  type: UnauthoriedErrorType,
});
