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

/** Namespaces searched for error codes, in this order. Plugins (e.g. enterprise) register their own, with prefixed codes. */
const NAMESPACES = ['server_errors', 'enterprise_server_errors'];
const LOOKUP_OPTIONS = { keySeparator: false, nsSeparator: false } as const;

const translateCode = (code: string, params: Record<string, unknown>): string | undefined => {
  const ns = NAMESPACES.find((namespace) => i18n.exists(code, { ns: namespace, ...LOOKUP_OPTIONS }));

  return ns ? (i18n.t(code, { ...params, ns, ...LOOKUP_OPTIONS }) as string) : undefined;
};

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

  if (typeof code !== 'string') {
    return undefined;
  }

  const params = body?.params && typeof body.params === 'object' ? (body.params as Record<string, unknown>) : {};

  return translateCode(code, params);
};

type ErrorDetail = { code?: string; message?: string; params?: Record<string, unknown> };
type ValidationResultBody = { errors?: Record<string, Array<string>>; error_details?: Record<string, Array<ErrorDetail>> };

const translateDetail = (detail: ErrorDetail): string | undefined => {
  return typeof detail.code === 'string' ? translateCode(detail.code, detail.params ?? {}) : undefined;
};

/**
 * Validation results of the server (\`{ errors: { field: [message] }, error_details: { field: [{ code, message, params }] } }\`)
 * with the messages of coded errors translated. Errors without a code or without a translation keep the English
 * message the server sent. The result has the same shape as the input, so forms keep reading \`errors\`.
 */
export const translateValidationResult = <T extends ValidationResultBody>(validation: T): T => {
  const details = validation?.error_details;

  if (!validation?.errors || !details) {
    return validation;
  }

  const errors = Object.fromEntries(
    Object.entries(validation.errors).map(([field, messages]) => [
      field,
      messages.map((message) => {
        const detail = details[field]?.find((candidate) => candidate.message === message);

        return (detail && translateDetail(detail)) ?? message;
      }),
    ]),
  );

  return { ...validation, errors };
};

export default translateApiError;
