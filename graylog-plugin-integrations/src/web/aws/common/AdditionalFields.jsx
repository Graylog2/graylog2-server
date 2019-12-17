import React, { useState } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

import { Icon } from 'components/common';

const AdditionalFields = ({ children, className, onToggle, title, visible }) => {
  const [fieldsVisible, setFieldsVisible] = useState(visible);

  const handleToggle = () => {
    setFieldsVisible(!fieldsVisible);
    onToggle(!fieldsVisible);
  };

  return (
    <div className={className}>
      <ToggleAdditionalFields onClick={handleToggle} type="button">
        {title} <Icon name={fieldsVisible ? 'angle-down' : 'angle-right'} fixedWidth />
      </ToggleAdditionalFields>

      <AdditionalFieldsContent visible={fieldsVisible}>
        {children}
      </AdditionalFieldsContent>
    </div>
  );
};

AdditionalFields.propTypes = {
  children: PropTypes.any.isRequired,
  title: PropTypes.string.isRequired,
  onToggle: PropTypes.func,
  visible: PropTypes.bool,
  className: PropTypes.string,
};

AdditionalFields.defaultProps = {
  onToggle: () => {},
  visible: false,
  className: undefined,
};

const AdditionalFieldsContent = styled.div`
  display: ${props => (props.visible ? 'block' : 'none')};
  padding: 0 100px 0 25px;
`;

const ToggleAdditionalFields = styled.button`
  border: 0;
  color: #16ace3;
  font-size: 14px;
  display: block;

  :hover {
    color: #5e123b;
    text-decoration: underline;
  }
`;

export default AdditionalFields;
