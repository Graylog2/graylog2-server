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
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import Routes from 'routing/Routes';
import { MenuItem, DeleteMenuItem, BootstrapModalConfirm } from 'components/bootstrap';
import { Spinner } from 'components/common';
import useScopePermissions from 'hooks/useScopePermissions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { useDeleteLookupTable } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { LookupTableEntity } from 'components/lookup-tables/types';
import { MoreActionsMenu } from 'components/common/MoreActions';

type ActionsProps = {
  lut: LookupTableEntity;
};

function Actions({ lut }: ActionsProps) {
  const [showDeleteModal, setShowDeleteModal] = useState<boolean>(false);
  const sendTelemetry = useSendTelemetry();
  const { deleteLookupTable, deletingLookupTable } = useDeleteLookupTable();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(lut);
  const navigate = useNavigate();

  const handleEdit = useCallback(() => {
    navigate(Routes.SYSTEM.LOOKUPTABLES.edit(lut.name));
  }, [lut, navigate]);

  const handleDelete = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.LUT.DELETED, {
      app_pathname: 'lut',
      app_section: 'lut',
    });

    deleteLookupTable(lut.id).then(() => setShowDeleteModal(false));
  }, [lut.id, sendTelemetry, deleteLookupTable]);

  if (loadingScopePermissions) return <Spinner text="" />;

  if (!scopePermissions.is_mutable) return null;

  return (
    <>
      <MoreActionsMenu id={lut.id} size="lg" pullRight title={`More Actions for ${lut.name}`}>
        <MenuItem onSelect={handleEdit}>Edit</MenuItem>
        <MenuItem divider />
        <DeleteMenuItem onSelect={() => setShowDeleteModal(true)}>Delete</DeleteMenuItem>
      </MoreActionsMenu>
      {showDeleteModal && (
        <BootstrapModalConfirm
          showModal
          title="Delete Lookup Table"
          onCancel={() => setShowDeleteModal(false)}
          onConfirm={handleDelete}
          cancelButtonDisabled={deletingLookupTable}
          confirmButtonDisabled={deletingLookupTable}
          confirmButtonText="Delete">
          <p>Are you sure you want to delete lookup table &quot;{lut.title}&quot;?</p>
        </BootstrapModalConfirm>
      )}
    </>
  );
}

const renderActions = (lut: LookupTableEntity) => <Actions lut={lut} />;

function useActions() {
  return {
    renderActions,
  };
}

export default useActions;
