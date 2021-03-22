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
import PropTypes from 'prop-types';

import { getValueFromInput } from 'util/FormsUtils';
import { Input } from 'components/bootstrap';
import { BooleanField as BooleanFieldType } from 'components/configurationforms/types';

type Props = {
  autoFocus?: boolean,
  field: BooleanFieldType,
  onChange: (title: string, value: boolean) => void,
  title: string,
  typeName: string,
  value?: boolean,
};

const BooleanField = ({ autoFocus, field, onChange, title, typeName, value }: Props) => {
  const handleChange = (event) => {
    const nextValue = getValueFromInput(event.target);

    onChange(title, nextValue);
  };

  return (
    <Input id={`${typeName}-${title}`}
           name={`configuration[${title}]`}
           type="checkbox"
           label={field.human_name}
           checked={value}
           help={field.description}
           onChange={handleChange}
           autoFocus={autoFocus} />
  );
};

BooleanField.propTypes = {
  autoFocus: PropTypes.bool,
  field: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  typeName: PropTypes.string.isRequired,
  value: PropTypes.bool,
};

BooleanField.defaultProps = {
  autoFocus: false,
  value: false,
};

export default BooleanField;
