import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

import formValidation from '../utils/formValidation';

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

const ValidatedInput = ({ help, onChange, id, label, fieldData, type, ...restProps }) => {
  const { dirty, error, value } = fieldData;

  const handleChange = (event) => {
    onChange(event, { dirty: true });
  };

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
           onChange={handleChange}
           onBlur={checkValidity}
           bsStyle={(error && dirty && 'error') || undefined}
           defaultValue={(type !== 'select' && value) || undefined}
           value={(type === 'select' && value) || undefined}
           label={<Label label={label} error={error} />}
           help={help} />
  );
};

ValidatedInput.propTypes = {
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
