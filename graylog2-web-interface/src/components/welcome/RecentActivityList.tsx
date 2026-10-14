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

import React, { useCallback, useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { ListGroup } from 'components/bootstrap';
import { DEFAULT_PAGINATION } from 'components/welcome/Constants';
import { NoSearchResult, PaginatedList, RelativeTime, Spinner, Link } from 'components/common';
import { StyledListGroupItem, TimeInfo } from 'components/welcome/EntityListItem';
import type { RecentActivityType } from 'components/welcome/types';
import useRecentActivity from 'components/welcome/hooks/useRecentActivity';
import getTitleForEntityType from 'util/getTitleForEntityType';
import { getValuesFromGRN } from 'logic/permissions/GRN';
import useHasEntityPermissionByGRN from 'hooks/useHasEntityPermissionByGRN';
import useShowRouteFromGRN from 'routing/hooks/useShowRouteFromGRN';

type Props = { itemGrn: string; activityType: RecentActivityType; itemTitle: string; userName?: string };

const ACTIVITY_TEXTS = {
  create: { anonymous: 'activity.create', byUser: 'activity.create_by' },
  update: { anonymous: 'activity.update', byUser: 'activity.update_by' },
  share: { anonymous: 'activity.share', byUser: 'activity.share_by' },
  unshare: { anonymous: 'activity.unshare', byUser: 'activity.unshare_by' },
  delete: { anonymous: 'activity.delete', byUser: 'activity.delete_by' },
} as const;

// Activity types added to the server later are shown with their technical name.
const UNKNOWN_ACTIVITY_TEXTS = { anonymous: 'activity.other', byUser: 'activity.other_by' } as const;

const ActionItem = ({ itemGrn, activityType, itemTitle, userName = null }: Props) => {
  const hasReadPermission = useHasEntityPermissionByGRN(itemGrn, 'read');
  const { id: itemId, type: itemType } = getValuesFromGRN(itemGrn);
  const { t } = useTranslation('welcome');
  const entityTypeTitle = getTitleForEntityType(itemType, false) ?? t('unsupported_type', { type: itemType });
  const entityLink = useShowRouteFromGRN(itemGrn);
  const activityTexts = ACTIVITY_TEXTS[activityType] ?? UNKNOWN_ACTIVITY_TEXTS;
  const entityTitle = itemTitle || itemId;
  const showLink = activityType !== 'delete' && !!entityLink && hasReadPermission;

  return (
    <div>
      <Trans
        t={t}
        i18nKey={userName ? activityTexts.byUser : activityTexts.anonymous}
        values={{ type: entityTypeTitle, user: userName, activity: activityType }}
        components={{ entity: !showLink ? <i>{entityTitle}</i> : <Link to={entityLink}>{entityTitle}</Link> }}
      />
    </div>
  );
};

const RecentActivityList = () => {
  const { t } = useTranslation('welcome');
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const {
    data: { recentActivity, total },
    isFetching,
  } = useRecentActivity(pagination);
  const onPageChange = useCallback(
    (newPage) => {
      setPagination((cur) => ({ ...cur, page: newPage }));
    },
    [setPagination],
  );

  if (isFetching) return <Spinner />;

  if (recentActivity.length === 0) {
    return (
      <NoSearchResult>
        {t('no_recent_activity')}
        <p>{t('no_recent_activity_hint')}</p>
      </NoSearchResult>
    );
  }

  return (
    <PaginatedList
      onChange={onPageChange}
      useQueryParameter={false}
      activePage={pagination.page}
      totalItems={total}
      pageSize={pagination.per_page}
      showPageSizeSelect={false}
      hideFirstAndLastPageLinks>
      <ListGroup>
        {recentActivity.map(({ id, timestamp, activityType, itemGrn, itemTitle, userName }) => (
          <StyledListGroupItem key={id}>
            <ActionItem itemGrn={itemGrn} activityType={activityType} itemTitle={itemTitle} userName={userName} />
            <TimeInfo>
              <RelativeTime dateTime={timestamp} />
            </TimeInfo>
          </StyledListGroupItem>
        ))}
      </ListGroup>
    </PaginatedList>
  );
};

export default RecentActivityList;
