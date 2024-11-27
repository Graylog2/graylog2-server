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
import styled, { css } from 'styled-components';

import { ListGroupItem } from 'components/bootstrap';

const StyledListGroupItem = styled(ListGroupItem)(({ theme }) => css`
  background-color: ${theme.colors.variant.lightest.default};
  font-size: ${theme.fonts.size.body};
  color: ${theme.utils.contrastingColor(theme.colors.variant.lightest.default)};
  padding: 0 15px;

  .form-group {
    margin: 0;
  }
`);

const HeaderWrapper = styled.div`
  padding: 10px 0;
  min-height: 40px;
`;

const ControlledTableListHeader = ({ children = '' }: { children?: React.ReactNode }) => {
  const wrapStringChildren = (text: string) => <HeaderWrapper>{text}</HeaderWrapper>;

  const header = typeof children === 'string' ? wrapStringChildren(children) : children;

  return <StyledListGroupItem>{header}</StyledListGroupItem>;
};

export default ControlledTableListHeader;
