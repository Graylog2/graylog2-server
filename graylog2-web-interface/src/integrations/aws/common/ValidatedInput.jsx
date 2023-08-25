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
import styled from 'styled-components';

import { Input } from 'components/bootstrap';
import formValidation from 'integrations/aws/utils/formValidation';

const Label = ({ label, error }) => {
  if (error) {
    return (
      <ErrorContainer>
        {label}
        <Error><i className="fa fa-exclamation-triangle" /> {error}</Error>
      </ErrorContainer>
    );
  }

  return label;
};

const ValidatedInput = ({ className, help, onChange, id, label, fieldData, type, ...restProps }) => {
  const { dirty, error, value } = fieldData;

  const checkValidity = (event) => {
    if (dirty) {
      const errorOutput = formValidation.checkInputValidity(event.target);

      onChange(event, { error: errorOutput });
    }
  };

  return (
    <Input {...restProps}
           id={id}
           type={type}
           onChange={onChange}
           onBlur={checkValidity}
           className={className}
           bsStyle={(error && dirty && 'error') || undefined}
           defaultValue={(type !== 'select' && value) || undefined}
           value={(type === 'select' && value) || undefined}
           label={<Label label={label} error={error} />}
           help={help} />
  );
};

ValidatedInput.propTypes = {
  className: PropTypes.string,
  fieldData: PropTypes.shape({
    error: PropTypes.string,
    dirty: PropTypes.bool,
    value: PropTypes.string,
  }),
  help: PropTypes.string,
  label: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
  ]).isRequired,
  id: PropTypes.string.isRequired,
  onChange: PropTypes.func,
  required: PropTypes.bool,
  type: PropTypes.string.isRequired,
};

ValidatedInput.defaultProps = {
  className: undefined,
  onChange: () => {},
  required: false,
  help: '',
  fieldData: {
    dirty: false,
    error: undefined,
    value: undefined,
  },
};

const Error = styled.span`
  display: block;
  font-weight: normal;
  padding-left: 15px;
  font-size: 0.85em;
`;

const ErrorContainer = styled.span`
  display: flex;
  align-items: center;
`;

export default ValidatedInput;
