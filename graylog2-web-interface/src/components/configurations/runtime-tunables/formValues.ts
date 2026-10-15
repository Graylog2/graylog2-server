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

import type { TunableSchema, TunableValues } from './types';
import { orderedProperties } from './schema';

export const toFormValues = (schema: TunableSchema, value: TunableValues): TunableValues =>
  Object.fromEntries(orderedProperties(schema).map(([name]) => [name, value?.[name]]));

export const changedProperties = (initialValues: TunableValues, values: TunableValues) =>
  Object.keys(values).filter((name) => !isEqual(initialValues?.[name], values[name]));
