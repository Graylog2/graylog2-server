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

import { Button, ButtonToolbar, DeleteMenuItem, MenuItem } from 'components/bootstrap';
import { MoreActions } from 'components/common/EntityDataTable';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import UserNotification from 'util/UserNotification';

import { deleteTranslationFeedback, updateTranslationFeedbackStatus } from './api';
import type { Status, TranslationFeedback } from './types';

const TranslationFeedbackActions = ({ feedback }: { feedback: TranslationFeedback }) => {
  const { t } = useTranslation('translation_feedback');
  const { refetch } = useTableFetchContext();

  const onError = (error: unknown) =>
    UserNotification.error(t('notifications.update_failed', { error }), t('notifications.update_failed_title'));

  const setStatus = (status: Status) =>
    updateTranslationFeedbackStatus(feedback.id, status).then(refetch).catch(onError);

  const onDelete = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('actions.confirm_delete', { text: feedback.text }))) {
      deleteTranslationFeedback(feedback.id).then(refetch).catch(onError);
    }
  };

  return (
    <ButtonToolbar>
      {feedback.status === 'open' ? (
        <Button bsSize="xs" onClick={() => setStatus('fixed')}>
          {t('actions.mark_fixed')}
        </Button>
      ) : (
        <Button bsSize="xs" onClick={() => setStatus('open')}>
          {t('actions.reopen')}
        </Button>
      )}
      <MoreActions>
        {feedback.status !== 'wont_fix' && (
          <MenuItem onSelect={() => setStatus('wont_fix')}>{t('actions.mark_wont_fix')}</MenuItem>
        )}
        <DeleteMenuItem onSelect={onDelete} />
      </MoreActions>
    </ButtonToolbar>
  );
};

export default TranslationFeedbackActions;
