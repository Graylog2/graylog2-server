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

import { Link, RelativeTime } from 'components/common';
import { ListGroupItem } from 'components/bootstrap';
import getTitleForEntityType from 'util/getTitleForEntityType';
import { getValuesFromGRN } from 'logic/permissions/GRN';
import useHasEntityPermissionByGRN from 'hooks/useHasEntityPermissionByGRN';
import useShowRouteFromGRN from 'routing/hooks/useShowRouteFromGRN';

export const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
`;

const Title = styled.span`
  text-transform: capitalize;
`;

export const TimeInfo = styled.span(
  ({ theme }) => css`
    flex-shrink: 0;
    white-space: nowrap;
    color: ${theme.colors.text.secondary};
  `,
);

type Props = {
  title: string;
  timestamp?: string;
  grn: string;
};

const EntityItem = ({ title, grn, timestamp = undefined }: Props) => {
  const { id, type } = getValuesFromGRN(grn);
  const hasReadPermission = useHasEntityPermissionByGRN(grn, 'read');
  const entityTypeTitle = useMemo(() => getTitleForEntityType(type, false) ?? 'unknown', [type]);
  const entityLink = useShowRouteFromGRN(grn);
  const entityTitle = title ?? id;
  const showLink = !!entityLink && hasReadPermission;

  return (
    <StyledListGroupItem>
      <Title>
        {`${entityTypeTitle} `}
        {!showLink ? <i>{entityTitle}</i> : <Link to={entityLink}>{entityTitle}</Link>}
      </Title>
      {timestamp ? (
        <TimeInfo>
          <RelativeTime dateTime={timestamp} />
        </TimeInfo>
      ) : null}
    </StyledListGroupItem>
  );
};

export default EntityItem;
