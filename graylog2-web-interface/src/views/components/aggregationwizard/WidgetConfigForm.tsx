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
import * as React from 'react';
import { Form, Formik, FormikProps } from 'formik';

import { TimeUnits } from 'views/Constants';

export type MetricFormValues = {
  function: string,
  field: string | undefined,
  name?: string | undefined,
  percentile?: number | undefined,
};

type GroupingField<T extends 'values' | 'time'> = {
  field: string | undefined
  type: T;
}

export type GroupingDirection = 'row' | 'column';

export type DateGrouping = {
  direction: GroupingDirection,
  field: GroupingField<'time'>,
  interval: {
    type: 'auto',
    scaling: number,
  } | {
    type: 'timeunit',
    value: number,
    unit: keyof typeof TimeUnits,
  }

}
export type ValuesGrouping = {
  direction: GroupingDirection,
  field: GroupingField<'values'>,
  limit: number,
};

export type GroupByFormValues = DateGrouping | ValuesGrouping;

export type VisualizationFormValues = {};

export type SortFormValues = {}
export interface WidgetConfigFormValues {
  metrics?: Array<MetricFormValues>,
  groupBy?: {
    columnRollup: boolean,
    groupings: Array<GroupByFormValues>,
  },
  visualization?: VisualizationFormValues,
  sort?: SortFormValues,
}

type Props = {
  children: ((props: FormikProps<WidgetConfigFormValues>) => React.ReactNode) | React.ReactNode,
  initialValues: WidgetConfigFormValues,
  onSubmit: (formValues: WidgetConfigFormValues) => void,
  validate: (formValues: WidgetConfigFormValues) => { [key: string]: string },
}

const WidgetConfigForm = ({ children, onSubmit, initialValues, validate }: Props) => {
  return (
    <Formik<WidgetConfigFormValues> initialValues={initialValues}
                                    validate={validate}
                                    enableReinitialize
                                    validateOnChange
                                    validateOnMount
                                    onSubmit={onSubmit}>
      {(...args) => (
        <Form className="form form-horizontal">
          {typeof children === 'function' ? children(...args) : children}
        </Form>
      )}
    </Formik>
  );
};

export default WidgetConfigForm;
