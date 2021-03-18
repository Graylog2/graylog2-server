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

import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import { COLORSCALES } from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';

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

export type DateGrouping = {
  direction: GroupingDirection,
  field: GroupingField<'time'>,
  interval: AutoTimeConfig | TimeUnitConfig,
}
export type ValuesGrouping = {
  direction: GroupingDirection,
  field: GroupingField<'values'>,
  limit: number,
};

export type GroupByFormValues = DateGrouping | ValuesGrouping;

export type AreaVisualizationConfigFormValues = {
  interpolation: 'linear' | 'step-after' | 'spline';
};

export type LineVisualizationConfigFormValues = {
  interpolation: 'linear' | 'step-after' | 'spline';
};

export type BarVisualizationConfigFormValues = {
  barmode: 'group' | 'stack' | 'relative' | 'overlay',
};

export type NumberVisualizationConfigFormValues = {
  trend: boolean,
  trend_preference: 'LOWER' | 'NEUTRAL' | 'HIGHER',
};

export type HeatMapVisualizationConfigFormValues = {
  colorScale: typeof COLORSCALES[number],
  reverseScale: boolean,
  autoScale: boolean,
  zMin: number,
  zMax: number
  useSmallestAsDefault: boolean,
  defaultValue: number,
};

export type VisualizationConfigFormValues = {};

export type VisualizationFormValues = {
  type: string,
  config?: VisualizationConfigFormValues,
};

export type VisualizationConfigDefinition = {
  fromConfig: (config: VisualizationConfig) => VisualizationConfigFormValues,
  toConfig: (formValues: VisualizationConfigFormValues) => VisualizationConfig,
  fields: Array<ConfigurationField>,
};

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

export interface WidgetConfigValidationErrors {
  metrics?: Array<{ [key: string]: string }>,
  groupBy?: Array<{ [key: string]: string }>,
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
          {typeof children === 'function' ? children(...args) : children}
        </Form>
      )}
    </Formik>
  );
};

export default WidgetConfigForm;
