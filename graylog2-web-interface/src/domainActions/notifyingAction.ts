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
import UserNotification from 'util/UserNotification';
import { createNotFoundError } from 'logic/errors/ReportedErrors';
import ErrorsActions from 'actions/errors/ErrorsActions';

type Notification = {
  title?: string,
  message?: string,
};

const _displaySuccessNotification = <T, Args extends Array<T>>(successNotification: (...Args) => Notification, ...args: Args) => {
  const { message, title } = successNotification(...args);
  UserNotification.success(message, title || 'Success');
};

const _displayErrorNotification = <T, Args extends Array<T>>(errorNotification: (error: string, ...Args) => Notification, error, ...args: Args) => {
  let errorMessage = String(error);

  if ((error?.status === 400 || error?.status === 500) && error?.additional?.body?.message) {
    errorMessage = `${error.additional.body.message} - ${error.message}`;
  }

  const { message, title } = errorNotification(errorMessage, ...args);
  UserNotification.error(message, title || 'Error');
};

type Props<F extends (...args: Array<any>) => any> = {
  action: F,
  success?: (...args: Parameters<F>) => Notification,
  error: (error: string, ...args: Parameters<F>) => Notification,
  notFoundRedirect?: boolean,
};

type PromiseResult<P extends Promise<any>> = P extends Promise<infer R> ? R : never;

const notifyingAction = <F extends (...args: Array<any>) => Promise<any>>({
  action,
  success: successNotification,
  error: errorNotification,
  notFoundRedirect,
}: Props<F>) => {
  return (...args: Parameters<typeof action>) => action(...args).then((result: PromiseResult<ReturnType<F>>) => {
    if (successNotification) _displaySuccessNotification(successNotification, ...args);

    return result;
  }).catch((error) => {
    if (notFoundRedirect && error?.status === 404) {
      ErrorsActions.report(createNotFoundError(error));
      throw error;
    }

    _displayErrorNotification(errorNotification, error, ...args);

    throw error;
  });
};

export default notifyingAction;
