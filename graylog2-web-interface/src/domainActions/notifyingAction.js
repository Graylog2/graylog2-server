// @flow strict
import UserNotification from 'util/UserNotification';
import { createNotFoundError } from 'logic/errors/ReportedErrors';

type Notification = {
  title?: string,
  message?: string,
};

type Props<Args, Result> = {
  action: (...Args) => Promise<Result | void>,
  successNotification?: (...Args) => Notification,
  errorNotification: (error: string, ...Args) => Notification,
  notFoundRedirect?: boolean,
};

const notifyingAction = <T, Args: Array<T>, Result>({ action, successNotification, errorNotification, notFoundRedirect }: Props<Args, Result>): (...Args) => Promise<Result | void> => {
  return (...args: Args): Promise<Result | void> => action(...args).then((result) => {
    if (successNotification) {
      const { message, title } = successNotification(...args);
      UserNotification.success(message, title || 'Success');
    }

    return result;
  }).catch((error) => {
    if (notFoundRedirect && error?.status === '404') {
      createNotFoundError(error);

      return;
    }

    const readableError = String(error); // Todo format error here correctly
    const { message, title } = errorNotification(readableError, ...args);
    UserNotification.error(message, title || 'Error');
  });
};

export default notifyingAction;
