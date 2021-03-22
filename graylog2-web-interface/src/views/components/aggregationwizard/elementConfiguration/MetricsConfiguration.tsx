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

import { Button, ButtonToolbar } from 'components/graylog';

import ElementConfigurationSection from './ElementConfigurationSection';
import Metric from './Metric';

import { WidgetConfigFormValues } from '../WidgetConfigForm';

const MetricsConfiguration = () => {
  const { values } = useFormikContext<WidgetConfigFormValues>();
  const { metrics } = values;

  return (
    <>
      <FieldArray name="metrics"
                  render={(arrayHelpers) => (
                    <>
                      <div>
                        {metrics.map((metric, index) => {
                          return (
                          // eslint-disable-next-line react/no-array-index-key
                            <ElementConfigurationSection key={`metrics-${index}`}>
                              <Metric index={index} />
                            </ElementConfigurationSection>
                          );
                        })}
                      </div>
                      <ButtonToolbar>
                        <Button className="pull-right" bsSize="small" type="button" onClick={() => arrayHelpers.push({})}>
                          Add a Metric
                        </Button>
                      </ButtonToolbar>
                    </>
                  )} />
    </>
  );
};

export default MetricsConfiguration;
