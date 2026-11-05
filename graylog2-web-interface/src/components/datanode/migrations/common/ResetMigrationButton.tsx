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
import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { Migration } from '@graylog/server-api';

import { ConfirmDialog } from 'components/common';
import { Button } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import { QUERY_KEY as DATA_NODES_CA_QUERY_KEY } from 'preflight/hooks/useDataNodesCA';
import { MIGRATION_STATE_QUERY_KEY } from 'components/datanode/hooks/useMigrationState';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const resetMigration = async () => Migration.resetState();

const ResetMigrationButton = () => {
  const { t } = useTranslation('collectors', { keyPrefix: 'datanode' });
  const queryClient = useQueryClient();
  const [showDialog, setShowDialog] = useState(false);
  const sendTelemetry = useSendTelemetry('migration');

  const { mutateAsync: onResetMigration } = useMutation({
    mutationFn: resetMigration,

    onSuccess: () => {
      UserNotification.success(t('reset_migration.success'));
      queryClient.invalidateQueries({ queryKey: DATA_NODES_CA_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: MIGRATION_STATE_QUERY_KEY });
    },

    onError: (error) => {
      UserNotification.error(t('reset_migration.failed', { error }), t('reset_migration.failed_title'));
    },
  });

  const handleResetClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.RESET_MIGRATION_CLICKED, {
      app_pathname: 'datanode',
    });

    setShowDialog(true);
  };

  const handleConfirmClick = async () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.RESET_MIGRATION_CONFIRM_CLICKED, {
      app_pathname: 'datanode',
    });

    await onResetMigration();
    setShowDialog(false);
  };

  return (
    <>
      <Button bsStyle="primary" bsSize="small" onClick={handleResetClick}>
        {t('reset_migration.button')}
      </Button>
      {showDialog && (
        <ConfirmDialog
          title={t('reset_migration.button')}
          show
          onConfirm={handleConfirmClick}
          onCancel={() => setShowDialog(false)}>
          {t('reset_migration.confirm')}
        </ConfirmDialog>
      )}
    </>
  );
};

export default ResetMigrationButton;
