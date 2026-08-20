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
import FetchError from 'logic/errors/FetchError';

/**
 * Extract a user-readable message from an unknown thrown value.
 *
 * - For FetchError, prefer `responseMessage` (the server's response body) over the formatted
 *   wrapper message. Falls back to `message` for non-400 statuses.
 * - For other Error instances, return `message`.
 * - For anything else (string, plain object, undefined), coerce to string.
 */
export const extractErrorMessage = (error: unknown): string => {
  if (error instanceof FetchError) {
    return error.responseMessage ?? error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }

  return String(error);
};

export default extractErrorMessage;
