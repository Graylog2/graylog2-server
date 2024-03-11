import * as React from 'react';

export type FilterComponent = {
  configuration: (editValue: unknown, onChange: (newValue: unknown, shouldSubmit: boolean) => void) => React.ReactNode,
  renderValue: (values: string) => React.ReactNode,
  valueFromConfig?: (value: string) => unknown,
  valueForConfig?: (value: unknown) => string,
  multiEdit?: boolean,
  submitChangesOnClose?: boolean,
}

export type FilterComponents = Record<string, FilterComponent>
