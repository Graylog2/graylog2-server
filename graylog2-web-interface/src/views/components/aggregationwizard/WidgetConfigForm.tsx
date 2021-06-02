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
import styled from 'styled-components';

import PropagateValidationState from 'views/components/aggregationwizard/PropagateValidationState';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import { AutoTimeConfig, TimeUnitConfig } from 'views/logic/aggregationbuilder/Pivot';

const StyledForm = styled(Form)`
  display: flex;
  width: 100%;
`;

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
  id: string,
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

export type VisualizationConfigDefinition<
  ConfigType extends VisualizationConfig = VisualizationConfig,
  ConfigFormValuesType extends VisualizationConfigFormValues = VisualizationConfigFormValues
  > = {
  fromConfig: (config: ConfigType | undefined) => ConfigFormValuesType,
  toConfig: (formValues: ConfigFormValuesType) => ConfigType,
  createConfig?: () => Partial<ConfigFormValuesType>,
  fields: Array<ConfigurationField>,
};

export type SortFormValues = {
  type?: 'metric' | 'groupBy',
  field?: string,
  direction?: 'Ascending' | 'Descending',
  id: string,
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
  sort?: Array<{ [key: string]: string }>,
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
        <StyledForm className="form form-horizontal">
          <PropagateValidationState />
          {typeof children === 'function' ? children(...args) : children}
        </StyledForm>
      )}
    </Formik>
  );
};

export default WidgetConfigForm;
