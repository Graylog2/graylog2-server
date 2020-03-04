import React, { useState } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

import { Icon } from 'components/common';
import { Button } from 'components/graylog';

const AdditionalFields = ({ children, className, onToggle, title, visible }) => {
  const [fieldsVisible, setFieldsVisible] = useState(visible);

  const handleToggle = () => {
    setFieldsVisible(!fieldsVisible);
    onToggle(!fieldsVisible);
  };

  return (
    <div className={className}>
      <ToggleAdditionalFields bsStyle="link" bsSize="xsmall" onClick={handleToggle} type="button">
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

const ToggleAdditionalFields = styled(Button)`
  border: 0;
  display: block;
  font-size: 14px;

  :hover {
    text-decoration: underline;
  }
`;

export default AdditionalFields;
