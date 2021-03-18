import * as React from 'react';
import { ConfigurationField } from 'views/types';

import SelectField from './configurationFields/SelectField';

type Props = {
  name: string,
  fields: Array<ConfigurationField>,
};

const VisualizationConfigurationOptions = ({ name, fields = [] }: Props) => {
  const configurationFields = fields.map((field) => {
    switch (field.type) {
      case 'select': return <SelectField name={name} field={field} />;
      default: throw new Error(`Invalid configuration field type: ${field.type}`);
    }
  });

  return <>{configurationFields}</>;
};

export default VisualizationConfigurationOptions;
