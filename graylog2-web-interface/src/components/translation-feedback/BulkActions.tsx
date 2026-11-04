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
import { useTranslation } from 'react-i18next';

import { MenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import UserNotification from 'util/UserNotification';

import type { BulkResult } from './api';
import { bulkDeleteTranslationFeedback, bulkUpdateTranslationFeedbackStatus } from './api';
import type { Status } from './types';

const BulkActions = () => {
  const { t } = useTranslation('translation_feedback');
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const { refetch } = useTableFetchContext();

  const onDone = ({ changed }: BulkResult) => {
    UserNotification.success(t('bulk.changed', { count: changed }));
    setSelectedEntities([]);
    refetch();
  };
  const onError = (error: unknown) =>
    UserNotification.error(t('notifications.update_failed', { error }), t('notifications.update_failed_title'));

  const setStatus = (status: Status) =>
    bulkUpdateTranslationFeedbackStatus(selectedEntities, status).then(onDone).catch(onError);

  const onDelete = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('bulk.confirm_delete', { count: selectedEntities.length }))) {
      bulkDeleteTranslationFeedback(selectedEntities).then(onDone).catch(onError);
    }
  };

  return (
    <BulkActionsDropdown>
      <MenuItem onSelect={() => setStatus('fixed')}>{t('actions.mark_fixed')}</MenuItem>
      <MenuItem onSelect={() => setStatus('wont_fix')}>{t('actions.mark_wont_fix')}</MenuItem>
      <MenuItem onSelect={() => setStatus('open')}>{t('actions.reopen')}</MenuItem>
      <MenuItem divider />
      <MenuItem onSelect={onDelete}>{t('bulk.delete')}</MenuItem>
    </BulkActionsDropdown>
  );
};

export default BulkActions;
