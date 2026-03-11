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
import { Field, getIn, useFormikContext } from 'formik';
import styled from 'styled-components';

import type { ConfigurationField } from 'views/types';
import type {
  VisualizationConfigFormValues,
  WidgetConfigFormValues,
} from 'views/components/aggregationwizard/WidgetConfigForm';
import { HoverForHelp } from 'components/common';
import TextField from 'views/components/aggregationwizard/visualization/configurationFields/TextField';

import BooleanField from './configurationFields/BooleanField';
import NumericField from './configurationFields/NumericField';
import SelectField from './configurationFields/SelectField';
import MultiSelectField from './configurationFields/MultiSelectField';

const TitleLabelWithHelp = styled.div`
  display: flex;
  align-items: center;
`;

const TitleHoverForHelp = styled((props: React.ComponentProps<typeof HoverForHelp>) => <HoverForHelp {...props} />)`
  margin-left: 5px;
`;

const componentForType = (type: string): React.FC<FieldComponentProps> => {
  switch (type) {
    case 'select':
      return SelectField;
    case 'boolean':
      return BooleanField;
    case 'numeric':
      return NumericField;
    case 'multi-select':
      return MultiSelectField;
    case 'text':
      return TextField;
    default:
      throw new Error(`Invalid configuration field type: ${type}`);
  }
};

const titleForField = (field: ConfigurationField) => {
  const { helpComponent: HelpComponent } = field;

  return HelpComponent ? (
    <TitleLabelWithHelp>
      {field.title}
      <TitleHoverForHelp title={`Help for ${field.title}`} placement="top">
        <HelpComponent />
      </TitleHoverForHelp>
    </TitleLabelWithHelp>
  ) : (
    <>{field.title}</>
  );
};

export type FieldComponentProps = {
  field: ConfigurationField;
  name: string;
  title: React.ReactElement;
  value: any;
  onChange: (e: React.ChangeEvent<any>) => void;
  error: string | undefined;
  values: any;
  inputHelp?: string;
};

type Props = {
  name: string;
  fields: Array<ConfigurationField> | undefined;
};

const VisualizationConfigurationOptions = ({ name: namePrefix, fields }: Props) => {
  const { values } = useFormikContext<WidgetConfigFormValues>();
  const visualizationConfig: VisualizationConfigFormValues = getIn(values, namePrefix);

  return (
    <>
      {fields
        ?.filter((field) => (field.isShown ? field.isShown(visualizationConfig, values) : true))
        .map((field) => {
          const title = titleForField(field);
          const inputHelp =
            typeof field.inputHelp === 'function' ? field.inputHelp(visualizationConfig, values) : undefined;

          const fieldKey = `${namePrefix}.${field.name}`;
          const fieldName = `${namePrefix}.${field.name}`;

          if (field?.type === 'custom' && field?.component) {
            const CustomFieldComponent = field.component;

            return (
              <CustomFieldComponent
                key={fieldKey}
                name={fieldName}
                field={field}
                title={title}
                inputHelp={inputHelp}
                id={field.id}
              />
            );
          }

          const Component = componentForType(field.type);

          return (
            <Field key={fieldKey} name={fieldName}>
              {({ field: { name, value, onChange }, meta: { error } }) => (
                <Component
                  key={fieldKey}
                  name={name}
                  value={value}
                  values={values}
                  onChange={onChange}
                  error={error}
                  field={field}
                  title={title}
                  inputHelp={inputHelp}
                />
              )}
            </Field>
          );
        })}
    </>
  );
};

export default VisualizationConfigurationOptions;
