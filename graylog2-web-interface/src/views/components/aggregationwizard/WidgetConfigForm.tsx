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
import { useRef, useEffect, useContext } from 'react';
import type { FormikProps } from 'formik';
import { Form, Formik } from 'formik';
import styled from 'styled-components';

import type { ConfigurationField } from 'views/types';
import WidgetEditApplyAllChangesContext from 'views/components/contexts/WidgetEditApplyAllChangesContext';
import PropagateDisableSubmissionState from 'views/components/aggregationwizard/PropagateDisableSubmissionState';
import type VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import type { AutoTimeConfig, TimeUnitConfig } from 'views/logic/aggregationbuilder/Pivot';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import { updateWidgetAggregationElements } from './AggregationWizard';

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

type Required<T, K extends keyof T> = Pick<T, K> & Partial<T>;

export interface WidgetConfigFormValues {
  metrics?: Array<MetricFormValues>,
  groupBy?: {
    columnRollup: boolean,
    groupings: Array<Required<GroupByFormValues, 'id'>>,
    rowLimit: string | number | undefined,
    columnLimit: string | number | undefined,
  },
  visualization?: VisualizationFormValues,
  sort?: Array<SortFormValues>,
  rowLimit?: string,
  columnLimit?: string,
}

export interface WidgetConfigValidationErrors {
  metrics?: Array<{ [key: string]: string }>,
  groupBy?: {
    groupings?: Array<{ [key: string]: string }>,
    rowLimit?: string,
    columnLimit?: string,
  },
  visualization?: { [key: string]: string | any },
  sort?: Array<{ [key: string]: string }>,
}

type Props = {
  children: ((props: FormikProps<WidgetConfigFormValues>) => React.ReactNode) | React.ReactNode,
  initialValues: WidgetConfigFormValues,
  onSubmit: (formValues: WidgetConfigFormValues) => void,
  validate: (formValues: WidgetConfigFormValues) => WidgetConfigValidationErrors,
  config: AggregationWidgetConfig,
}

const useBindApplyElementConfigurationChanges = (formRef, config) => {
  const { bindApplyElementConfigurationChanges } = useContext(WidgetEditApplyAllChangesContext);

  useEffect(() => {
    bindApplyElementConfigurationChanges(() => {
      if (formRef.current) {
        const { dirty, values, isValid } = formRef.current;

        if (dirty && isValid) {
          return updateWidgetAggregationElements(values, config);
        }
      }

      return undefined;
    });
  }, [formRef, bindApplyElementConfigurationChanges, config]);
};

const WidgetConfigForm = ({ children, onSubmit, initialValues, validate, config }: Props) => {
  const formRef = useRef(null);

  useBindApplyElementConfigurationChanges(formRef, config);

  return (
    <Formik<WidgetConfigFormValues> initialValues={initialValues}
                                    validate={validate}
                                    enableReinitialize
                                    innerRef={formRef}
                                    validateOnChange
                                    validateOnMount
                                    onSubmit={onSubmit}>
      {(...args) => (
        <StyledForm className="form form-horizontal">
          <PropagateDisableSubmissionState formKey="widget-config" disableSubmission={!args[0].isValid || args[0].isValidating || args[0].isSubmitting} />
          {typeof children === 'function' ? children(...args) : children}
        </StyledForm>
      )}
    </Formik>
  );
};

export default WidgetConfigForm;
