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
import styled, { css } from 'styled-components';

import { Table } from 'components/bootstrap';
import { DEFAULT_PAGINATION, entityTypeMap } from 'components/welcome/Constants';
import { EmptyResult, PaginatedList, Spinner } from 'components/common';
import { relativeDifference } from 'util/DateTime';
import Routes from 'routing/Routes';
import { Link } from 'components/common/router';
import { StyledLabel } from 'components/welcome/EntityListItem';
import { useRecentActivity } from 'components/welcome/hooks';
import type { EntityItemType, RecentActivityType } from 'components/welcome/types';

const ActionItemLink = styled(Link)(({ theme }) => css`
  color: ${theme.colors.variant.primary};
  &:hover {
    color: ${theme.colors.variant.darker.primary};
  }
`);

type Props = { itemType: EntityItemType, itemId: string, activityType: RecentActivityType, itemTitle: string, userName?: string };

const ActionItem = ({ itemType, itemId, activityType, itemTitle, userName }: Props) => {
  const { typeTitle } = entityTypeMap[itemType];
  const entityLink = useMemo(() => {
    return entityTypeMap[itemType] ? Routes.pluginRoute(entityTypeMap[itemType].link)(itemId) : undefined;
  }, [itemId, itemType]);

  return (
    <div>
      {`The ${typeTitle} `}
      <ActionItemLink target="_blank" to={entityLink}>{itemTitle}</ActionItemLink>
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
      <EmptyResult>
        There is no recent activity yet.
        <p />
        Whenever any other user will update content you have access to, or share new content with you, it will show up here.
      </EmptyResult>
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
                      <StyledLabel title={timestamp} bsStyle="primary">{relativeDifference(timestamp)}
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
