// @flow strict
import UserNotification from 'util/UserNotification';
import { createNotFoundError } from 'logic/errors/ReportedErrors';
import ErrorsActions from 'actions/errors/ErrorsActions';
import type { ListenableAction } from 'stores/StoreTypes';

type Notification = {
  title?: string,
  message?: string,
};

const _displaySuccessNotification = <T, Args: Array<T>>(successNotification: (...Args) => Notification, ...args: Args) => {
  const { message, title } = successNotification(...args);
  UserNotification.success(message, title || 'Success');
};

const _displayErrorNotification = <T, Args: Array<T>>(errorNotification: (error: string, ...Args) => Notification, error, ...args: Args) => {
  let errorMessage = String(error);

  if ((error?.status === 400 || error?.status === 500) && error?.additional?.body?.message) {
    errorMessage = `${error.additional.body.message} - ${error.message}`;
  }

  const { message, title } = errorNotification(errorMessage, ...args);
  UserNotification.error(message, title || 'Error');
};

type Props<Args, ActionResult> = {
  action: ListenableAction<(...Args) => ActionResult>,
  success?: (...Args) => Notification,
  error: (error: string, ...Args) => Notification,
  notFoundRedirect?: boolean,
};

const notifyingAction = <T, Args: Array<T>, Result, ActionResult: Promise<Result>>({
  action,
  success: successNotification,
  error: errorNotification,
  notFoundRedirect,
}: Props<Args, ActionResult>): (...Args) => ActionResult => {
  return (...args: Args): ActionResult => action(...args).then((result) => {
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
