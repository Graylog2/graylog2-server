import UserNotification from 'preflight/util/UserNotification';

export function onError<T, E = Error>(fn: () => Promise<T>, handler: (e: E) => void) {
  return async () => {
    try {
      return await fn();
    } catch (e) {
      handler(e);
      throw e;
    }
  };
}

export function defaultOnError<T>(fn: () => Promise<T>, message: string, title: string) {
  return onError(fn, (error: Error) => UserNotification.error(`${message}: ${error}`, title));
}
