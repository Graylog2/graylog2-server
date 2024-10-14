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
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import { Link } from 'components/common/router';
import { ListGroupItem, Label } from 'components/bootstrap';
import getTitleForEntityType from 'util/getTitleForEntityType';
import { RelativeTime } from 'components/common';
import { getValuesFromGRN } from 'logic/permissions/GRN';
import useHasEntityPermissionByGRN from 'hooks/useHasEntityPermissionByGRN';
import useShowRouteFromGRN from 'routing/hooks/useShowRouteFromGRN';

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  gap: 16px;
  align-items: flex-start;
`;

export const StyledLabel = styled(Label)`
  cursor: default;
  width: 110px;
  display: block;
`;

const LastOpenedTime = styled.i(({ theme }: { theme: DefaultTheme }) => css`
  color: ${theme.colors.gray[60]};
`);

type Props = {
  title: string,
  timestamp?: string,
  grn: string,
}

const EntityItem = ({ title, grn, timestamp }: Props) => {
  const { id, type } = getValuesFromGRN(grn);
  const hasReadPermission = useHasEntityPermissionByGRN(grn, 'read');
  const entityTypeTitle = useMemo(() => getTitleForEntityType(type, false) ?? 'unknown', [type]);
  const entityLink = useShowRouteFromGRN(grn);
  const entityTitle = title || id;
  const showLink = !!entityLink && hasReadPermission;

  return (
    <StyledListGroupItem>
      <StyledLabel bsStyle="info">{entityTypeTitle}</StyledLabel>
      {!showLink
        ? <i>{entityTitle}</i>
        : <Link to={entityLink}>{entityTitle}</Link>}
      {timestamp ? <LastOpenedTime><RelativeTime dateTime={timestamp} /></LastOpenedTime> : null}
    </StyledListGroupItem>
  );
};

export default EntityItem;
