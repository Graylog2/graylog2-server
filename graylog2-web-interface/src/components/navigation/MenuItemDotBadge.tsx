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
import styled from 'styled-components';

const Badge = styled.span`
  position: relative;

  &::after {
    display: 'block';
    content: ' ';
    position: absolute;
    width: 8px;
    height: 8px;

    background-color: ${({ theme }) => theme.colors.brand.primary};
    border-radius: 50%;
    top: 0;
    right: -12px;
  }
`;
type Props = {
  text: string;
  title: string;
  showDot: boolean;
};

const MenuItemDotBadge = ({ text, title, showDot }: Props) => {
  if (!showDot) {
    return <span>{text}</span>;
  }

  return <Badge title={title}>{text}</Badge>;
};

export default MenuItemDotBadge;
