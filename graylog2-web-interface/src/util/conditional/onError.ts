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
