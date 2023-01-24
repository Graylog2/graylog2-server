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
import { ListGroupItem, Label } from 'components/bootstrap';
import type { EntityItemType } from 'components/welcome/types';
import getTitleForEntityType from 'util/getTitleForEntityType';
import getShowRouteForEntity from 'routing/getShowRouteForEntity';

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
  const entityTypeTitle = useMemo(() => {
    try {
      return getTitleForEntityType(type);
    } catch (e) {
      return 'unknown';
    }
  }, [type]);
  const entityLink = useMemo(() => {
    try {
      return getShowRouteForEntity(id, type);
    } catch (e) {
      return undefined;
    }
  }, [type, id]);
  const entityTitle = title || id;

  return (
    <StyledListGroupItem>
      <StyledLabel bsStyle="info">{entityTypeTitle}</StyledLabel>
      {!entityLink
        ? <i>{entityTitle}</i>
        : <Link target="_blank" to={entityLink}>{entityTitle}</Link>}
    </StyledListGroupItem>
  );
};

export default EntityItem;
