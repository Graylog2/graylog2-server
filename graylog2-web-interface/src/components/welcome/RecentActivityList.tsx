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

import React, { useCallback, useMemo, useState } from 'react';

import { Table } from 'components/bootstrap';
import { DEFAULT_PAGINATION } from 'components/welcome/Constants';
import { NoSearchResult, PaginatedList, RelativeTime, Spinner } from 'components/common';
import { Link } from 'components/common/router';
import { StyledLabel } from 'components/welcome/EntityListItem';
import type { RecentActivityType } from 'components/welcome/types';
import useRecentActivity from 'components/welcome/hooks/useRecentActivity';
import getTitleForEntityType from 'util/getTitleForEntityType';
import { getValuesFromGRN } from 'logic/permissions/GRN';
import useHasEntityPermissionByGRN from 'hooks/useHasEntityPermissionByGRN';
import useShowRouteFromGRN from 'routing/hooks/useShowRouteFromGRN';

type Props = { itemGrn: string, activityType: RecentActivityType, itemTitle: string, userName?: string };

const ActionItem = ({ itemGrn, activityType, itemTitle, userName = null }: Props) => {
  const hasReadPermission = useHasEntityPermissionByGRN(itemGrn, 'read');
  const { id: itemId, type: itemType } = getValuesFromGRN(itemGrn);
  const entityTypeTitle = useMemo(() => getTitleForEntityType(itemType, false) ?? `(unsupported type ${itemType})`, [itemType]);
  const entityLink = useShowRouteFromGRN(itemGrn);
  const entityTitle = itemTitle || itemId;
  const showLink = activityType !== 'delete' && !!entityLink && hasReadPermission;

  return (
    <div>
      {`The ${entityTypeTitle} `}
      {!showLink
        ? <i>{entityTitle}</i>
        : <Link target="_blank" to={entityLink}>{entityTitle}</Link>}
      {' was '}
      {`${activityType}d`}
      {userName ? ` by ${userName}` : ''}
    </div>
  );
};

const RecentActivityList = () => {
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { data: { recentActivity, total }, isFetching } = useRecentActivity(pagination);
  const onPageChange = useCallback((newPage) => {
    setPagination((cur) => ({ ...cur, page: newPage }));
  }, [setPagination]);

  if (isFetching) return <Spinner />;

  if (recentActivity.length === 0) {
    return (
      <NoSearchResult>
        There is no recent activity yet.
        <p>
          Whenever any other user will update content you have access to, or share new content with you, it will show up here.
        </p>
      </NoSearchResult>
    );
  }

  return (
    <PaginatedList onChange={onPageChange} useQueryParameter={false} activePage={pagination.page} totalItems={total} pageSize={pagination.per_page} showPageSizeSelect={false} hideFirstAndLastPageLinks>
      <Table striped>
        <tbody>
          {
            recentActivity.map(({ id, timestamp, activityType, itemGrn, itemTitle, userName }) => (
              <tr key={id}>
                <td style={{ width: 110 }}>
                  <StyledLabel bsStyle="primary">
                    <RelativeTime dateTime={timestamp} />
                  </StyledLabel>
                </td>
                <td>
                  <ActionItem itemGrn={itemGrn} activityType={activityType} itemTitle={itemTitle} userName={userName} />
                </td>
              </tr>
            ))
          }
        </tbody>
      </Table>
    </PaginatedList>
  );
};

export default RecentActivityList;
