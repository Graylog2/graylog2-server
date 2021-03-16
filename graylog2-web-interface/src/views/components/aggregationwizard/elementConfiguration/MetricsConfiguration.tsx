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
import { useFormikContext, FieldArray } from 'formik';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import Metric from './Metric';

import type { WidgetConfigFormValues } from '../WidgetConfigForm';

// eslint-disable-next-line @typescript-eslint/no-redeclare
interface WidgetConfigFormValues {

}

type Props = {
  config: AggregationWidgetConfig,
  onConfigChange: (newConfig: AggregationWidgetConfig) => void
}

const MetricsConfiguration = ({ config, onConfigChange }: Props) => {
  const { values } = useFormikContext<WidgetConfigFormValues>();
  const metrics = values.metrics ?? [{}];

  return (
    <>
      <FieldArray name="metrics"
                  render={(arrayHelpers) => (
                    <>
                      {metrics.map((metric, index) => {
                        return (
                          <div>
                            <Metric index={index} />
                          </div>
                        );
                      })}
                      <button type="button" onClick={() => arrayHelpers.push({})}>
                        Add a Metric
                      </button>
                    </>
                  )} />
    </>
  );
};

export default MetricsConfiguration;
