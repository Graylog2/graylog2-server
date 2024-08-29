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
import { useMutation } from '@tanstack/react-query';

import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

const deleteLicense = async (id : string) => {
  const url = qualifyUrl(ApiRoutes.IndexSetsApiController.deleteFailedSnapshot(id).url);

  return fetch('DELETE', url);
};

const useDeleteFailedSnapshotMutation = (id: string) => {
  const remove = useMutation(() => deleteLicense(id), {
    onError: (errorThrown) => {
      UserNotification.error(`Snapshot deletion failed: ${errorThrown}`,
        'Could not delete snapshot');
    },
    onSuccess: () => {
      UserNotification.success('Snapshot has been successfully deleted.', 'Success!');
    },
  });

  return { deleteFailedSnapshot: remove.mutateAsync };
};

export default useDeleteFailedSnapshotMutation;
