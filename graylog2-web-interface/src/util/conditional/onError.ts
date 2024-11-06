import UserNotification from 'preflight/util/UserNotification';

export async function onError<T, E = Error>(promise: Promise<T>, handler: (e: E) => void) {
  try {
    return await promise;
  } catch (e) {
    handler(e);
    throw e;
  }
}

export function defaultOnError<T>(promise: Promise<T>, message: string, title: string) {
  return onError(promise, (error: Error) => UserNotification.error(`${message}: ${error}`, title));
}
