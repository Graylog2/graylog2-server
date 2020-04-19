// @flow strict
// eslint-disable-next-line import/no-cycle
import { FetchError } from 'logic/rest/FetchProvider';

export const ReactErrorType = 'ReactError';
export const UnauthoriedErrorType = 'UnauthorizedError';

type ReactErrorInternal = {
  error: Error,
  info: { componentStack: string },
  type: 'ReactError'
}

type UnauthorizedErrorInternal = {
  error: FetchError,
  type: 'UnauthorizedError'
}

export type ReportedError = ReactErrorInternal | UnauthorizedErrorInternal

export const ReactError = (error: $PropertyType<ReactErrorInternal, 'error'>, info: $PropertyType<ReactErrorInternal, 'info'>): ReactErrorInternal => ({
  error,
  info,
  type: ReactErrorType,
});

export const UnauthorizedError = (error: $PropertyType<UnauthorizedErrorInternal, 'error'>): UnauthorizedErrorInternal => ({
  error,
  type: UnauthoriedErrorType,
});
