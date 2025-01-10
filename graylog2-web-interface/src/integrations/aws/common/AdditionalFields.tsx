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
import styled, { css } from 'styled-components';

import { Icon } from 'components/common';
import { Button } from 'components/bootstrap';

const AdditionalFieldsContent = styled.div<{ visible: boolean}>(({ visible }) => css`
  display: ${visible ? 'block' : 'none'};
  padding: 0 100px 0 25px;
`);

const ToggleAdditionalFields = styled(Button)`
  border: 0;
  display: block;
  font-size: 14px;

  &:hover {
    text-decoration: underline;
  }
`;

type AdditionalFieldsProps = {
  children: any;
  title: string;
  onToggle?: (...args: any[]) => void;
  visible?: boolean;
  className?: string;
};

const AdditionalFields = ({
  children,
  className,
  onToggle = () => {},
  title,
  visible = false,
}: AdditionalFieldsProps) => {
  const [fieldsVisible, setFieldsVisible] = useState(visible);

  const handleToggle = () => {
    setFieldsVisible(!fieldsVisible);
    onToggle(!fieldsVisible);
  };

  return (
    <div className={className}>
      <ToggleAdditionalFields bsStyle="link" bsSize="xsmall" onClick={handleToggle} type="button">
        {title} <Icon name={fieldsVisible ? 'keyboard_arrow_down' : 'chevron_right'} />
      </ToggleAdditionalFields>

      <AdditionalFieldsContent visible={fieldsVisible}>
        {children}
      </AdditionalFieldsContent>
    </div>
  );
};

export default AdditionalFields;
