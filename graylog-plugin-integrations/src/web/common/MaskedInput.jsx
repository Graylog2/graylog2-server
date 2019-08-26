import React, { useState } from 'react';
import PropTypes from 'prop-types';

import styled from 'styled-components';

import ValidatedInput from './ValidatedInput';

const MaskedInput = ({ label, ...props }) => {
  const [masked, setMasked] = useState(true);
  const toggleLabel = (
    <LabelWrapper>
      {label}
      <ToggleMask onClick={() => setMasked(!masked)} ariaDescription={`Toggle ${label} field input`}>
        <i className={`fa fa-${masked ? 'eye-slash' : 'eye'}`} />
      </ToggleMask>
    </LabelWrapper>
  );

  return (
    <ValidatedInput {...props} type={masked ? 'password' : 'text'} label={toggleLabel} />
  );
};

MaskedInput.propTypes = {
  label: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
  ]).isRequired,
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
