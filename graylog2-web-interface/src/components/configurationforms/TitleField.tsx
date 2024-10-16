import React from 'react';

import { TextField } from 'components/configurationforms';

import type { TextField as TextFieldType } from './types';

type Props = {
  helpText?: string,
  onChange?: (title: string, value: string, dirty?: boolean) => void
  typeName: string,
  value?: string
};

const TitleField = ({ typeName, helpText = '', value = '', onChange = () => {} }: Props) => {
  const titleField: TextFieldType = {
    is_optional: false,
    attributes: [],
    human_name: 'Title',
    description: helpText,
    is_encrypted: false,
    additional_info: {},
    default_value: '',
    position: 0,
    type: 'text',
  };

  return (
    <TextField key={`${typeName}-title`}
               typeName={typeName}
               title="title"
               field={titleField}
               value={value}
               onChange={onChange}
               autoFocus />
  );
};

export default TitleField;
