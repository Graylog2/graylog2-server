// @flow strict
import UserNotification from 'util/UserNotification';
import { createNotFoundError } from 'logic/errors/ReportedErrors';
import ErrorsActions from 'actions/errors/ErrorsActions';

type Notification = {
  title?: string,
  message?: string,
};

type Props<Args, Result> = {
  action: (...Args) => Promise<Result | void>,
  success?: (...Args) => Notification,
  error: (error: string, ...Args) => Notification,
  notFoundRedirect?: boolean,
};

const notifyingAction = <T, Args: Array<T>, Result>({ action, success: successNotification, error: errorNotification, notFoundRedirect }: Props<Args, Result>): (...Args) => Promise<Result | void> => {
  return (...args: Args): Promise<Result | void> => action(...args).then((result) => {
    if (successNotification) {
      const { message, title } = successNotification(...args);
      UserNotification.success(message, title || 'Success');
    }

    return result;
  }).catch((error) => {
    let readableError = String(error);

    if (notFoundRedirect && error?.status === 404) {
      ErrorsActions.report(createNotFoundError(error));

      return;
    }

    if (error?.status === 400 && error?.additional?.body?.message) {
      readableError = error.additional.body.message;
    }

    const { message, title } = errorNotification(readableError, ...args);
    UserNotification.error(message, title || 'Error');
  });
};

export default notifyingAction;
