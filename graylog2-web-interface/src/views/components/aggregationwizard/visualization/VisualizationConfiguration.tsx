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
import { useCallback, useMemo } from 'react';
import { Field, useFormikContext } from 'formik';
import styled from 'styled-components';

import { Input, Checkbox } from 'components/bootstrap';
import Select from 'components/common/Select';
import usePluginEntities from 'hooks/usePluginEntities';
import { defaultCompare } from 'logic/DefaultCompare';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import { TIMESTAMP_FIELD } from 'views/Constants';

import VisualizationConfigurationOptions from './VisualizationConfigurationOptions';
import VisualizationElement from './VisualizationElement';

import ElementConfigurationContainer from '../ElementConfigurationContainer';

const EventAnnotationCheckbox = styled(Checkbox)`
  input[type="checkbox"] {
    margin-right: 0;
    right: 0;
  }
`;

const isTimeline = (values: WidgetConfigFormValues) => {
  if (!values.groupBy?.groupings || values.groupBy.groupings.length === 0) {
    return false;
  }

  const firstRowGrouping = values.groupBy.groupings.find((grouping) => grouping.direction === 'row');

  return firstRowGrouping?.field?.field === TIMESTAMP_FIELD;
};

const VisualizationConfiguration = () => {
  const visualizationTypes = usePluginEntities('visualizationTypes');
  const findVisualizationType = useCallback((type: string) => visualizationTypes
    .find((visualizationType) => visualizationType.type === type), [visualizationTypes]);

  const visualizationTypeOptions = useMemo(() => visualizationTypes
    .sort((v1, v2) => defaultCompare(v1.displayName, v2.displayName))
    .map(({ displayName, type }) => ({ label: displayName, value: type })), [visualizationTypes]);

  const { values, setFieldValue } = useFormikContext<WidgetConfigFormValues>();
  const currentVisualizationType = findVisualizationType(values.visualization.type);

  const setNewVisualizationType = useCallback((newValue: string) => {
    const type = findVisualizationType(newValue);
    const createConfig = type.config?.createConfig ?? (() => ({}));

    setFieldValue('visualization', {
      type: newValue,
      config: createConfig(),
    }, true);
  }, [findVisualizationType, setFieldValue]);

  const isTimelineChart = isTimeline(values);
  const supportsEventAnnotations = currentVisualizationType.capabilities?.includes('event-annotations') ?? false;

  return (
    <ElementConfigurationContainer elementTitle={VisualizationElement.title}>
      <Field name="visualization.type">
        {({ field: { name, value }, meta: { error } }) => (
          <Input id="visualization-type-select"
                 label="Type"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={visualizationTypeOptions}
                    aria-label="Select visualization type"
                    clearable={false}
                    name={name}
                    value={value}
                    menuPortalTarget={document.body}
                    onChange={(newValue: string) => {
                      if (newValue !== value) {
                        setNewVisualizationType(newValue);
                      }
                    }}
                    size="small" />
          </Input>
        )}
      </Field>
      {isTimelineChart && supportsEventAnnotations && (
        <Field name="visualization.eventAnnotation">
          {({ field: { name, value = false }, meta: { error } }) => (
            <Input id={`${name}-input`}
                   label="Show Event annotations"
                   error={error}
                   labelClassName="col-sm-11"
                   wrapperClassName="col-sm-1">
              <EventAnnotationCheckbox id={`${name}-input`}
                                       name={name}
                                       onChange={() => setFieldValue(name, !value)}
                                       checked={value}
                                       className="pull-right" />
            </Input>
          )}
        </Field>

      )}
      <VisualizationConfigurationOptions name="visualization.config" fields={currentVisualizationType.config?.fields ?? []} />
    </ElementConfigurationContainer>
  );
};

export default VisualizationConfiguration;
