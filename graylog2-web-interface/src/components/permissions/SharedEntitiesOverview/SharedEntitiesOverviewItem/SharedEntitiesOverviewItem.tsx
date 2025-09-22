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

import useHasEntityPermissionByGRN from 'hooks/useHasEntityPermissionByGRN';
import { RestrictedAccessTooltip } from 'components/common';
import { Link } from 'components/common/router';
import type SharedEntity from 'logic/permissions/SharedEntity';
import useShowRouteFromGRN from 'routing/hooks/useShowRouteFromGRN';
import usePluggableSharedEntityTableElements from 'hooks/usePluggableSharedEntityTableElements';

import OwnersCell from './OwnersCell';

type Props = {
  capabilityTitle: string;
  sharedEntity: SharedEntity;
};

const NameColumnWrapper = styled.div`
  display: flex;
  align-items: center;
`;

const SharedEntitiesOverviewItem = ({ capabilityTitle, sharedEntity: { owners, title, type, id } }: Props) => {
  const entityRoute = useShowRouteFromGRN(id);
  const { getPluggableTableCells, pluggableAttributes } = usePluggableSharedEntityTableElements();
  const hasReadPermission = useHasEntityPermissionByGRN(id, 'read');
  const hasEditPermission = useHasEntityPermissionByGRN(id, 'edit');
  const hasRequiredPermission = () => {
    switch (type) {
      case 'user':
      case 'team':
        return hasEditPermission;

      default:
        return hasReadPermission;
    }
  };

  return (
    <tr key={title + type}>
      <td className="limited">
        <NameColumnWrapper>
          {hasRequiredPermission() ? (
            <Link to={entityRoute}>{title}</Link>
          ) : (
            <>
              {title}
              <RestrictedAccessTooltip entityName={type} capabilityName="view" />
            </>
          )}
        </NameColumnWrapper>
      </td>
      <td className="limited">{type}</td>
      <OwnersCell owners={owners} />
      <td className="limited">{capabilityTitle}</td>
      {pluggableAttributes && getPluggableTableCells(id, type)}
    </tr>
  );
};

export default SharedEntitiesOverviewItem;
