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
import { ConfigurationField } from 'views/types';

import PropagateValidationState from 'views/components/aggregationwizard/PropagateValidationState';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import { AutoTimeConfig, TimeUnitConfig } from 'views/logic/aggregationbuilder/Pivot';

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

export type BaseGrouping = {
  direction: GroupingDirection,
};

export type DateGrouping = BaseGrouping & {
  field: GroupingField<'time'>,
  interval: AutoTimeConfig | TimeUnitConfig,
};

export type ValuesGrouping = BaseGrouping & {
  field: GroupingField<'values'>,
  limit: number,
};

export type GroupByFormValues = DateGrouping | ValuesGrouping;

export type VisualizationConfigFormValues = {};

export type VisualizationFormValues = {
  type: string,
  config?: VisualizationConfigFormValues,
  eventAnnotation?: boolean,
};

export type VisualizationConfigDefinition = {
  fromConfig: (config: VisualizationConfig | undefined) => VisualizationConfigFormValues,
  toConfig: (formValues: VisualizationConfigFormValues) => VisualizationConfig,
  createConfig?: () => Partial<VisualizationConfigFormValues>,
  fields: Array<ConfigurationField>,
};

export type SortFormValues = {
  type: 'metric' | 'groupBy',
  field: string,
  direction: 'Ascending' | 'Descending',
}

export interface WidgetConfigFormValues {
  metrics?: Array<MetricFormValues>,
  groupBy?: {
    columnRollup: boolean,
    groupings: Array<GroupByFormValues>,
  },
  visualization?: VisualizationFormValues,
  sort?: Array<SortFormValues>,
}

export interface WidgetConfigValidationErrors {
  metrics?: Array<{ [key: string]: string }>,
  groupBy?: { groupings: Array<{ [key: string]: string }> },
  visualization?: { [key: string]: string | any },
  sort?: { [key: string]: string },
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
          <PropagateValidationState />
          {typeof children === 'function' ? children(...args) : children}
        </Form>
      )}
    </Formik>
  );
};

export default WidgetConfigForm;
