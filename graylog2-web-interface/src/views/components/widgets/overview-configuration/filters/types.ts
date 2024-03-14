import type * as React from 'react';

export type Filter = {
  field: string,
  value: Array<string>,
}

export type FilterComponent = {
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
}>
