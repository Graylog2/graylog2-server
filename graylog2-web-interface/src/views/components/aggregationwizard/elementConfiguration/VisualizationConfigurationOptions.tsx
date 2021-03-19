import * as React from 'react';
import { Field, getIn, useFormikContext } from 'formik';
import { ConfigurationField } from 'views/types';

import { VisualizationConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import BooleanField from 'views/components/aggregationwizard/elementConfiguration/configurationFields/BooleanField';
import { HoverForHelp } from 'components/common';

import InputField from './configurationFields/InputField';
import SelectField from './configurationFields/SelectField';

type Props = {
  name: string,
  fields: Array<ConfigurationField>,
};

const NumericField = (props) => <InputField type="number" {...props} />;

const componentForType = (type: string) => {
  switch (type) {
    case 'select': return SelectField;
    case 'boolean': return BooleanField;
    case 'numeric': return NumericField;
    default: throw new Error(`Invalid configuration field type: ${type}`);
  }
};

const titleForField = (field: ConfigurationField) => {
  const { helpComponent: HelpComponent } = field;

  return HelpComponent
    ? <>{field.title}<HoverForHelp title={`Help for ${field.title}`}><HelpComponent /></HoverForHelp></>
    : field.title;
};

export type FieldComponentProps = {
  field: ConfigurationField,
  name: string,
  title: React.ReactNode,
  value: any,
  onChange: (e: React.ChangeEvent<any>) => void,
  error: string | undefined,
}

const VisualizationConfigurationOptions = ({ name: namePrefix, fields = [] }: Props) => {
  const { values } = useFormikContext();
  const visualizationConfig: VisualizationConfigFormValues = getIn(values, namePrefix);
  const configurationFields = fields
    .filter((field) => (field.isShown ? field.isShown(visualizationConfig) : true))
    .map((field) => {
      const Component = componentForType(field.type);

      const title = titleForField(field);

      return (
        <Field key={`${namePrefix}.${field.name}`} name={`${namePrefix}.${field.name}`}>
          {({ field: { name, value, onChange }, meta: { error } }) => (
            <Component key={`${namePrefix}.${field.name}`} name={name} value={value} onChange={onChange} error={error} field={field} title={title} />
          )}
        </Field>
      );
    });

  return <>{configurationFields}</>;
};

export default VisualizationConfigurationOptions;
