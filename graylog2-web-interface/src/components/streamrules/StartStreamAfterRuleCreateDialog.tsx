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
import { Trans, useTranslation } from 'react-i18next';

import { ConfirmDialog } from 'components/common';

type Props = {
  show: boolean;
  streamTitle?: string;
  onConfirm: () => void;
  onCancel: () => void;
  isSubmitting?: boolean;
};

const StartStreamAfterRuleCreateDialog = ({
  show,
  streamTitle = '',
  onConfirm,
  onCancel,
  isSubmitting = false,
}: Props) => {
  const { t } = useTranslation('streams');

  return (
    <ConfirmDialog
      show={show}
      title={t('stream_rules.start_dialog.title')}
      btnConfirmText={t('stream_rules.start_dialog.confirm')}
      submitLoadingText={t('stream_rules.start_dialog.loading')}
      isAsyncSubmit
      isSubmitting={isSubmitting}
      onConfirm={onConfirm}
      onCancel={onCancel}>
      {t('stream_rules.start_dialog.paused')}{' '}
      {streamTitle ? (
        <Trans
          t={t}
          i18nKey="stream_rules.start_dialog.ask_named"
          values={{ title: streamTitle }}
          components={{ streamTitle: <strong /> }}
        />
      ) : (
        t('stream_rules.start_dialog.ask_unnamed')
      )}
    </ConfirmDialog>
  );
};

export default StartStreamAfterRuleCreateDialog;
