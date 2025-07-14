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
import { useDeleteCache } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { CacheEntity } from 'components/lookup-tables/types';

type ActionsProps = {
  cache: CacheEntity;
};

function Actions({ cache }: ActionsProps) {
  const [showDeleteModal, setShowDeleteModal] = React.useState<boolean>(false);
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();
  const { deleteCache, deletingCache } = useDeleteCache();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(cache);

  const handleEdit = React.useCallback(() => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(cache.name));
  }, [history, cache.name]);

  const handleDelete = React.useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.LUT.CACHE_DELETED, {
      app_pathname: 'lut',
      app_section: 'lut_cache',
    });

    deleteCache(cache.id).then(() => setShowDeleteModal(false));
  }, [cache.id, sendTelemetry, deleteCache]);

  if (loadingScopePermissions) return <Spinner text="" />;

  if (!scopePermissions.is_mutable) return null;

  return (
    <>
      <DropdownButton
        bsStyle="transparent"
        title={<Icon name="more_horiz" size="lg" />}
        id={cache.id}
        buttonTitle={cache.id}
        noCaret
        pullRight>
        <MenuItem onSelect={handleEdit}>Edit</MenuItem>
        <MenuItem divider />
        <DeleteMenuItem onSelect={() => setShowDeleteModal(true)}>Delete</DeleteMenuItem>
      </DropdownButton>
      {showDeleteModal && (
        <BootstrapModalConfirm
          showModal
          title="Delete Cache"
          onCancel={() => setShowDeleteModal(false)}
          onConfirm={handleDelete}
          cancelButtonDisabled={deletingCache}
          confirmButtonDisabled={deletingCache}
          confirmButtonText="Delete">
          <p>Are you sure you want to delete the cache &quot;{cache.title}&quot;?</p>
        </BootstrapModalConfirm>
      )}
    </>
  );
}

function useActions() {
  return {
    renderActions: (cache: CacheEntity) => <Actions cache={cache} />,
  };
}

export default useActions;
