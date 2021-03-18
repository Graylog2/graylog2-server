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
import { Field, useFormikContext } from 'formik';

import { Input } from 'components/bootstrap';
import { Checkbox } from 'components/graylog';
import Select from 'components/common/Select';
import usePluginEntities from 'views/logic/usePluginEntities';
import { defaultCompare } from 'views/logic/DefaultCompare';
import VisualizationConfigurationOptions from 'views/components/aggregationwizard/elementConfiguration/VisualizationConfigurationOptions';
import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import { TIMESTAMP_FIELD } from 'views/Constants';

const isTimeline = (values: WidgetConfigFormValues) => {
  return values.groupBy?.length > 0 && values.groupBy?.find((pivot) => pivot.type === 'row')?.field === TIMESTAMP_FIELD;
};

const VisualizationConfiguration = () => {
  const visualizationTypes = usePluginEntities('visualizationTypes');

  const visualizationTypeOptions = visualizationTypes.sort((v1, v2) => defaultCompare(v1.displayName, v2.displayName))
    .map(({ displayName, type }) => ({ label: displayName, value: type }));

  const { values, setFieldValue } = useFormikContext<WidgetConfigFormValues>();
  const currentVisualizationType = visualizationTypes.find((visualizationType) => visualizationType.type === values.visualization.type);

  const isTimelineChart = isTimeline(values);

  return (
    <div>
      <Field name="visualization.type">
        {({ field: { name, value, onChange }, meta: { error } }) => (
          <Input id="visualization-type-select"
                 label="Type"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={visualizationTypeOptions}
                    clearable={false}
                    name={name}
                    value={value}
                    onChange={(newValue) => {
                      if (newValue !== value) {
                        setFieldValue('visualization.config', {});
                        onChange({ target: { name, value: newValue } });
                      }
                    }} />
          </Input>
        )}
      </Field>
      {isTimelineChart && (
        <Field name="visualization.eventAnnotation">
          {({ field: { name, value, onChange }, meta: { error } }) => (
            <Input id={`${name}-input`}
                   label="Show Event annotations"
                   error={error}
                   labelClassName="col-sm-11"
                   wrapperClassName="col-sm-1">
              <Checkbox id={`${name}-input`} name={name} onChange={onChange} checked={value} />
            </Input>
          )}
        </Field>

      )}
      <VisualizationConfigurationOptions name="visualization.config" fields={currentVisualizationType.config?.fields ?? []} />
    </div>
  );
};

export default VisualizationConfiguration;
