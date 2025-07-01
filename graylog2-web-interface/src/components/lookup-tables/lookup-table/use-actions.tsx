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

import { MenuItem, DeleteMenuItem, DropdownButton, BootstrapModalConfirm } from 'components/bootstrap';
import { Icon, Spinner } from 'components/common';
import useScopePermissions from 'hooks/useScopePermissions';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import Routes from 'routing/Routes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { useDeleteLookupTable } from 'components/lookup-tables/hooks/useLookupTablesAPI';

import type { LookupTableEntity } from './types';

type ActionsProps = {
  lut: LookupTableEntity;
};

function Actions({ lut }: ActionsProps) {
  const [showDeleteModal, setShowDeleteModal] = React.useState<boolean>(false);
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();
  const { deleteLookupTable, deletingLookupTable } = useDeleteLookupTable();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(lut);

  const handleEdit = React.useCallback(() => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.edit(lut.name));
  }, [history, lut.name]);

  const handleDelete = React.useCallback(() => {
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
      <DropdownButton bsStyle="transparent" title={<Icon name="more_horiz" size="lg" />} id={lut.id} noCaret pullRight>
        <MenuItem onSelect={handleEdit}>Edit</MenuItem>
        <MenuItem divider />
        <DeleteMenuItem onSelect={() => setShowDeleteModal(true)}>Delete</DeleteMenuItem>
      </DropdownButton>
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

function useActions() {
  return {
    renderActions: (lut: LookupTableEntity) => <Actions lut={lut} />,
  };
}

export default useActions;
