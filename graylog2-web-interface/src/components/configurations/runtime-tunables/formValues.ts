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

import isEqual from 'lodash/isEqual';

import type { SecretValue, TunableSchema, TunableValues } from './types';
import { orderedProperties } from './schema';

const toSecretFormValue = (value: SecretValue | undefined): SecretValue =>
  value && 'is_set' in value && value.is_set ? { keep_value: true } : { set_value: '' };

export const toFormValues = (schema: TunableSchema, value: TunableValues): TunableValues =>
  Object.fromEntries(
    orderedProperties(schema).map(([name, property]) => [
      name,
      property['x-graylog-secret'] ? toSecretFormValue(value?.[name] as SecretValue) : value?.[name],
    ]),
  );

export const changedProperties = (initialValues: TunableValues, values: TunableValues) =>
  Object.keys(values).filter((name) => !isEqual(initialValues?.[name], values[name]));
