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
import i18n from 'i18n';

const NAMESPACE = 'server_errors';
const LOOKUP_OPTIONS = { ns: NAMESPACE, keySeparator: false, nsSeparator: false } as const;

type CodedBody = { code?: unknown; message?: unknown; params?: unknown };

const bodyOf = (error: unknown): CodedBody | undefined => {
  const body = (error as { additional?: { body?: unknown } } | undefined)?.additional?.body;

  return body && typeof body === 'object' ? (body as CodedBody) : undefined;
};

/**
 * The message of an error from the server in the language of the user, or `undefined` when the server did not send an
 * error code or there is no translation for it. The server always sends the English message, too, so callers fall
 * back to what they showed before.
 *
 * Codes and their texts live in the `server_errors` namespace, which uses the code as key and the English message with
 * `{{param}}` placeholders as value.
 */
const translateApiError = (error: unknown): string | undefined => {
  const body = bodyOf(error);
  const code = body?.code;

  if (typeof code !== 'string' || !i18n.exists(code, LOOKUP_OPTIONS)) {
    return undefined;
  }

  const params = body?.params && typeof body.params === 'object' ? (body.params as Record<string, unknown>) : {};

  return i18n.t(code, { ...params, ...LOOKUP_OPTIONS }) as string;
};

export default translateApiError;
