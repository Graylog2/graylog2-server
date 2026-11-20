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

export const DEFAULT_FIELD_LIMIT = 1000;
export const MAX_FIELD_LIMIT = 2000;

const TEMPLATE_TYPES_WITHOUT_FIELD_LIMIT = ['failures', 'events', 'collector_logs'];

export type FieldLimitErrors = {
  field_limit?: string;
  dynamic_mapping_disabled?: string;
};

type FieldLimitValues = {
  field_limit?: number | null;
  dynamic_mapping_disabled?: boolean | null;
  kept_fields_count?: number | null;
};

export const isFieldLimitAllowed = (templateType?: string) =>
  !TEMPLATE_TYPES_WITHOUT_FIELD_LIMIT.includes(templateType);

export const isIlluminateTemplateType = (templateType?: string) => templateType === 'illuminate_content';

export const prepareFieldLimitValues = <T extends FieldLimitValues>(values: T, allowed: boolean = true): T => {
  const { field_limit, dynamic_mapping_disabled, kept_fields_count: _keptFieldsCount, ...rest } = values;

  if (!allowed) {
    return rest as T;
  }

  return {
    ...rest,
    field_limit: typeof field_limit === 'number' ? field_limit : null,
    dynamic_mapping_disabled: dynamic_mapping_disabled ?? null,
  } as T;
};

const FIELD_ERROR_PATTERNS: Array<[keyof FieldLimitErrors, RegExp]> = [
  ['field_limit', /field[ _]?limit/i],
  ['dynamic_mapping_disabled', /dynamic[ _]?mapping/i],
];

const TEMPLATE_ERROR_MESSAGE = /^Invalid value for field \[([^\]]+)\]:\s*(.*)$/;

type ErrorEntry = { path?: string; message?: string };

const toErrorEntries = (body: unknown): Array<ErrorEntry> => {
  if (Array.isArray(body)) {
    return body;
  }

  const message = (body as { message?: unknown })?.message;

  if (typeof message !== 'string') {
    return [];
  }

  const [, path, text] = message.match(TEMPLATE_ERROR_MESSAGE) ?? [];

  return [{ path: path ?? message, message: text ?? message }];
};

export const extractFieldLimitErrors = (error: unknown): FieldLimitErrors => {
  const entries = toErrorEntries((error as { additional?: { body?: unknown } })?.additional?.body);

  return Object.fromEntries(
    FIELD_ERROR_PATTERNS.flatMap(([field, pattern]) => {
      const match = entries.find(({ path }) => pattern.test(path ?? ''));

      return match ? [[field, match.message]] : [];
    }),
  );
};
