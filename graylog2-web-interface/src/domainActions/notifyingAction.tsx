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
import UserNotification from 'util/UserNotification';
import { createNotFoundError } from 'logic/errors/ReportedErrors';
import ErrorsActions from 'actions/errors/ErrorsActions';
import type { ListenableAction } from 'stores/StoreTypes';

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

type Props<Args extends any[], ActionResult extends Promise<any>> = {
  action: ListenableAction<(...args: Args) => ActionResult>,
  success?: (...Args) => Notification,
  error: (error: string, ...Args) => Notification,
  notFoundRedirect?: boolean,
};

const notifyingAction = <T, Args extends Array<T>, Result, ActionResult extends Promise<Result>>({
  action,
  success: successNotification,
  error: errorNotification,
  notFoundRedirect,
}: Props<Args, ActionResult>): (...Args) => Promise<Result> => {
  return (...args: Args): Promise<Result> => action(...args).then((result) => {
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
