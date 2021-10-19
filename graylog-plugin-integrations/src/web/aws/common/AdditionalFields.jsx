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
import React, { useState } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { Icon } from 'components/common';
import { Button } from 'components/bootstrap';

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
  display: ${(props) => (props.visible ? 'block' : 'none')};
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
