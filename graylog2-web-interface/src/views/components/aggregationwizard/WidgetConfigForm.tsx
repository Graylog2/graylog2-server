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

export type MetricFormValues = {
  function: string,
  field: string | undefined,
  name?: string | undefined,
  percentile?: number | undefined,
};

export type GroupingDirection = 'row' | 'column';

export type DateGrouping = {
  direction: GroupingDirection,
  field: string,
  interval: {
    type: 'auto',
    scaling: number,
  } | {
    type: 'timeunit',
    value: number,
    unit: string,
  }

}

export type ValuesGrouping = {
  direction: GroupingDirection,
  field: string,
  limit: number,
};

export type GroupByFormValues = DateGrouping | ValuesGrouping;

export type VisualizationFormValues = {};

export type SortFormValues = {}
export interface WidgetConfigFormValues {
  metrics?: Array<MetricFormValues>,
  groupBy?: Array<GroupByFormValues>,
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
