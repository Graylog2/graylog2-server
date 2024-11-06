import UserNotification from 'preflight/util/UserNotification';

export async function onError<T, E = Error>(promise: Promise<T>, handler: (e: E) => void) {
  try {
    return await promise;
  } catch (e) {
    handler(e);
    throw e;
  }
}

export async function onSettled<T, R, E = Error>(promise: Promise<T>, handleSuccess: (r: T) => R, handleError: (e: E) => void) {
  try {
    const result = await promise;
    handleSuccess(result);

    return result;
  } catch (e) {
    handleError(e);
    throw e;
  }
}

export function defaultOnError<T>(promise: Promise<T>, message: string, title?: string) {
  return onError(promise, (error: Error) => UserNotification.error(`${message}: ${error}`, title));
}
