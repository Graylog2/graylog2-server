// @flow strict
import UserNotification from 'util/UserNotification';
import { createNotFoundError } from 'logic/errors/ReportedErrors';
import ErrorsActions from 'actions/errors/ErrorsActions';
import type { ListenableAction } from 'stores/StoreTypes';

type Notification = {
  title?: string,
  message?: string,
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
    if (successNotification) {
      const { message, title } = successNotification(...args);
      UserNotification.success(message, title || 'Success');
    }

    return result;
  }).catch((error) => {
    let readableError = String(error);

    if (notFoundRedirect && error?.status === 404) {
      ErrorsActions.report(createNotFoundError(error));
      throw error;
    }

    if ((error?.status === 400 || error?.status === 500) && error?.additional?.body?.message) {
      readableError = `${error.additional.body.message} - ${error.message}`;
    }

    const { message, title } = errorNotification(readableError, ...args);
    UserNotification.error(message, title || 'Error');

    throw error;
  });
};

export default notifyingAction;
