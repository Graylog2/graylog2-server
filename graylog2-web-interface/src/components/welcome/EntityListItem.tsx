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

import React, { useMemo } from 'react';
import styled from 'styled-components';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { ListGroupItem, Label } from 'components/bootstrap';
import { entityTypeMap } from 'components/welcome/Constants';
import type { EntityItemType } from 'components/welcome/types';

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  gap: 16px;
`;

export const StyledLabel = styled(Label)`
  cursor: default;
  width: 110px;
  display: block;
`;

type Props = {
  id: string,
  title: string,
  type: EntityItemType,
}

const EntityItem = ({ type, title, id }: Props) => {
  const entityLink = useMemo(() => {
    return entityTypeMap[type] ? Routes.pluginRoute(entityTypeMap[type].link)(id) : undefined;
  }, [type, id]);

  return (
    <StyledListGroupItem>
      <StyledLabel bsStyle="info">{entityTypeMap[type].typeTitle}</StyledLabel>
      <Link to={entityLink}>{title}</Link>
    </StyledListGroupItem>
  );
};

export default EntityItem;
