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
import type { EntityItemType, RecentActivityType } from 'components/welcome/types';
import useRecentActivity from 'components/welcome/hooks/useRecentActivity';
import getShowRouteForEntity from 'routing/getShowRouteForEntity';
import getTitleForEntityType from 'util/getTitleForEntityType';

type Props = { itemType: EntityItemType, itemId: string, activityType: RecentActivityType, itemTitle: string, userName?: string };

const ActionItem = ({ itemType, itemId, activityType, itemTitle, userName }: Props) => {
  const entityTypeTitle = useMemo(() => {
    try {
      return getTitleForEntityType(itemType);
    } catch (e) {
      return `(unsupported type ${itemType})`;
    }
  }, [itemType]);
  const entityLink = useMemo(() => {
    try {
      return getShowRouteForEntity(itemId, itemType);
    } catch (e) {
      return undefined;
    }
  }, [itemType, itemId]);
  const entityTitle = itemTitle || itemId;

  return (
    <div>
      {`The ${entityTypeTitle} `}
      {activityType === 'delete' || !entityLink
        ? <i>{entityTitle}</i>
        : <Link target="_blank" to={entityLink}>{entityTitle}</Link>}
      {' was '}
      {`${activityType}d`}
      {userName ? ` by ${userName}` : ''}
    </div>
  );
};

ActionItem.defaultProps = {
  userName: null,
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
            recentActivity.map(({ id, timestamp, activityType, itemType, itemId, itemTitle, userName }) => {
              return (
                <tr key={id}>
                  <td style={{ width: 110 }}>
                    <StyledLabel bsStyle="primary">
                      <RelativeTime dateTime={timestamp} />
                    </StyledLabel>
                  </td>
                  <td>
                    <ActionItem itemId={itemId} activityType={activityType} itemTitle={itemTitle} itemType={itemType} userName={userName} />
                  </td>
                </tr>
              );
            })
          }
        </tbody>
      </Table>
    </PaginatedList>
  );
};

export default RecentActivityList;
