import type * as React from 'react';

export type Filter = {
  field: string,
  value: Array<string>,
}

export type FilterComponent = {
  attribute: string,
  configuration: (selectedValues: Array<string>, editValue: unknown, onChange: (newValue: unknown, shouldSubmit: boolean) => void) => React.ReactNode,
  renderValue: (values: string) => React.ReactNode,
  valueFromConfig?: (value: string) => unknown,
  valueForConfig?: (value: unknown) => string,
  multiEdit?: boolean,
  submitChangesOnClose?: boolean,
}

export type FilterComponents = Array<FilterComponent>

export type Attributes = Array<{
  attribute: string,
  title: string,
  displayValue: (value: string) => React.ReactNode
}>
