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
