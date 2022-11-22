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
import styled from 'styled-components';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { ListGroupItem, Label } from 'components/bootstrap';
import { typeLinkMap } from 'components/welcome/helpers';
import type { LastOpenedItem } from 'components/welcome/types';

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  gap: 16px;
`;

export const StyledLabel = styled(Label)`
  cursor: default;
  width: 85px;
  display: block;
`;

const EntityItem = ({ type, title, id }: LastOpenedItem) => {
  return (
    <StyledListGroupItem>
      <StyledLabel bsStyle="info">{type}</StyledLabel>
      <Link target="_blank" to={Routes.pluginRoute(typeLinkMap[type].link)(id)}>{title}</Link>
    </StyledListGroupItem>
  );
};

export default EntityItem;
