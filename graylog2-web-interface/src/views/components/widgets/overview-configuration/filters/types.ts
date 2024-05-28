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
import type * as React from 'react';

export type Filter = {
  field: string,
  value: Array<string>,
}

export type FilterComponent = {
  allowMultipleValues?: boolean,
  attribute: string,
  configuration: (selectedValues: Array<string>, editValue: unknown, onChange: (newValue: unknown, shouldSubmit?: boolean) => void) => React.ReactNode,
  multiEdit?: boolean,
  renderValue?: (values: string) => React.ReactNode,
  submitChangesOnClose?: boolean,
  valueForConfig?: (value: unknown) => string,
  valueFromConfig?: (value: string) => unknown,
}

export type FilterComponents = Array<FilterComponent>

export type Attributes = Array<{
  attribute: string,
  displayValue?: (value: unknown) => React.ReactNode,
  sortable?: boolean,
  title: string,
  useCondition?: () => boolean,
}>
