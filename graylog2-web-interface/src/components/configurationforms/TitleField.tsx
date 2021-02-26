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
import PropTypes from 'prop-types';
import React from 'react';

import { TextField } from 'components/configurationforms';

type Props = {
  helpBlock?: React.ReactNode,
  onChange: () => void,
  typeName: string,
  value: string,
};

const TitleField = ({ typeName, helpBlock, value, onChange }: Props) => {
  const titleField = { is_optional: false, attributes: [], human_name: 'Title', description: helpBlock };

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

TitleField.propTypes = {
  helpBlock: PropTypes.node,
  onChange: PropTypes.func,
  typeName: PropTypes.string.isRequired,
  value: PropTypes.string,
};

TitleField.defaultProps = {
  helpBlock: <span />,
  onChange: () => {},
  value: '',
};

export default TitleField;
