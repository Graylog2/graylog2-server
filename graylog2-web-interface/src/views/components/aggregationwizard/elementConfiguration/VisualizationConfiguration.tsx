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
import Select from 'components/common/Select';
import usePluginEntities from 'views/logic/usePluginEntities';
import { defaultCompare } from 'views/logic/DefaultCompare';
import VisualizationConfigurationOptions
  from 'views/components/aggregationwizard/elementConfiguration/VisualizationConfigurationOptions';
import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

const VisualizationConfiguration = () => {
  const visualizationTypes = usePluginEntities('visualizationTypes');

  const visualizationTypeOptions = visualizationTypes.sort((v1, v2) => defaultCompare(v1.displayName, v2.displayName))
    .map(({ displayName, type }) => ({ label: displayName, value: type }));

  const { values } = useFormikContext<WidgetConfigFormValues>();
  const currentVisualizationType = visualizationTypes.find((visualizationType) => visualizationType.type === values.visualization.type);

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
                      onChange({ target: { name, value: newValue } });
                    }} />
          </Input>
        )}
      </Field>
      <VisualizationConfigurationOptions name="visualization.config" fields={currentVisualizationType.config?.fields ?? []} />
    </div>
  );
};

export default VisualizationConfiguration;
