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
import * as React from 'react';
import { $PropertyType } from 'utility-types';
import styled, { StyledComponent } from 'styled-components';

import Grantee from 'logic/permissions/Grantee';
import { ThemeInterface } from 'theme';
import { Icon } from 'components/common';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  display: inline-flex;
  align-items: center;
  justify-content: center;

  height: 30px;
  width: 30px;

  border-radius: 50%;
  background-color: ${theme.colors.gray[80]};
`);

type Props = {
  type: $PropertyType<Grantee, 'type'>,
};

const _iconName = (type) => {
  switch (type) {
    case 'global':
      return 'building';
    case 'team':
      return 'users';
    case 'error':
      return 'exclamation';
    case 'user':
    default:
      return 'user';
  }
};

const GranteeIcon = ({ type, ...rest }: Props) => (
  <Container {...rest}>
    <Icon name={_iconName(type)} />
  </Container>
);

export default GranteeIcon;
