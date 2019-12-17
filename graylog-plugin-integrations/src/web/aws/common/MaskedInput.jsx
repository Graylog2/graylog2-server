import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Icon } from 'components/common';

import ValidatedInput from './ValidatedInput';

const MaskedInput = ({ className, label, ...props }) => {
  const [masked, setMasked] = useState(true);
  const toggleLabel = (
    <LabelWrapper>
      {label}
      <ToggleMask onClick={() => setMasked(!masked)} ariaDescription={`Toggle ${label} field input`}>
        <Icon name={masked ? 'eye-slash' : 'eye'} />
      </ToggleMask>
    </LabelWrapper>
  );

  return (
    <ValidatedInput {...props} type={masked ? 'password' : 'text'} label={toggleLabel} formGroupClassName={className} />
  );
};

MaskedInput.propTypes = {
  label: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
  ]).isRequired,
  className: PropTypes.string,
};

MaskedInput.defaultProps = {
  className: undefined,
};

const LabelWrapper = styled.span`
  display: flex;
  align-items: center;
`;

const ToggleMask = styled.button`
  border: 0;
  background: none;
  padding: 0;
  margin: 0 0 0 12px;
`;

export default MaskedInput;
