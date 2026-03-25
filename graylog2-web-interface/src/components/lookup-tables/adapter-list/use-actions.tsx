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
import { useDeleteDataAdapter } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { DataAdapterEntity } from 'components/lookup-tables/types';
import { MoreActionsMenu } from 'components/common/MoreActions';

type ActionsProps = {
  adapter: DataAdapterEntity;
};

function Actions({ adapter }: ActionsProps) {
  const [showDeleteModal, setShowDeleteModal] = useState<boolean>(false);
  const sendTelemetry = useSendTelemetry();
  const { deleteDataAdapter, deletingDataAdapter } = useDeleteDataAdapter();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(adapter);
  const navigate = useNavigate();

  const handleEdit = useCallback(() => {
    navigate(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(adapter.name));
  }, [adapter, navigate]);

  const handleDelete = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.LUT.DATA_ADAPTER_DELETED, {
      app_pathname: 'lut',
      app_section: 'lut_data_adapter',
    });

    deleteDataAdapter(adapter.id).then(() => setShowDeleteModal(false));
  }, [adapter.id, sendTelemetry, deleteDataAdapter]);

  if (loadingScopePermissions) return <Spinner text="" />;

  if (!scopePermissions.is_mutable) return null;

  return (
    <>
      <MoreActionsMenu id={adapter.id} size="lg" pullRight title={`More Actions for ${adapter.name}`}>
        <MenuItem onSelect={handleEdit}>Edit</MenuItem>
        <MenuItem divider />
        <DeleteMenuItem onSelect={() => setShowDeleteModal(true)}>Delete</DeleteMenuItem>
      </MoreActionsMenu>
      {showDeleteModal && (
        <BootstrapModalConfirm
          showModal
          title="Delete Data Adapter"
          onCancel={() => setShowDeleteModal(false)}
          onConfirm={handleDelete}
          cancelButtonDisabled={deletingDataAdapter}
          confirmButtonDisabled={deletingDataAdapter}
          confirmButtonText="Delete">
          <p>Are you sure you want to delete the data adapter &quot;{adapter.title}&quot;?</p>
        </BootstrapModalConfirm>
      )}
    </>
  );
}

const renderActions = (adapter: DataAdapterEntity) => <Actions adapter={adapter} />;

function useActions() {
  return {
    renderActions,
  };
}

export default useActions;
