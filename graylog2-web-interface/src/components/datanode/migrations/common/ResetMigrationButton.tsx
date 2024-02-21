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

import fetch from 'logic/rest/FetchProvider';
import { ConfirmDialog } from 'components/common';
import { Button } from 'components/bootstrap';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'preflight/util/UserNotification';

const resetMigration = async () => {
  try {
    await fetch('DELETE', qualifyUrl('/migration/state'));

    UserNotification.success('Migration reset successfully.');
  } catch (errorThrown) {
    UserNotification.error(`Resetting migration failed with status: ${errorThrown}`, 'Could not reset the migration.');
  }
};

const ResetMigrationButton = () => {
  const [showDialog, setShowDialog] = useState(false);

  return (
    <>
      <Button bsStyle="primary" bsSize="small" onClick={() => setShowDialog(true)}>
        Reset Migration
      </Button>
      {showDialog && (
        <ConfirmDialog title="Reset Migration"
                       show
                       onConfirm={() => {
                         resetMigration();
                         setShowDialog(false);
                       }}
                       onCancel={() => setShowDialog(false)}>
          Are you sure you want to reset the migration?
        </ConfirmDialog>
      )}
    </>
  );
};

export default ResetMigrationButton;
