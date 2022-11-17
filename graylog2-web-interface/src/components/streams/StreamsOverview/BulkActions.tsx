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
import { uniq } from 'lodash';
import { Button } from 'react-bootstrap';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';

import StringUtils from 'util/StringUtils';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import type FetchError from 'logic/errors/FetchError';
import UserNotification from 'util/UserNotification';

type Props = {
  selectedStreamIds: Array<string>,
  setSelectedStreamIds: (streamIds: Array<string>) => void,
}

const BulkActions = ({ selectedStreamIds, setSelectedStreamIds }: Props) => {
  const queryClient = useQueryClient();
  const selectedItemsAmount = selectedStreamIds?.length;
  const descriptor = StringUtils.pluralize(selectedItemsAmount, 'stream', 'streams');

  const onDelete = useCallback(() => {
    if (window.confirm(`Do you really want to remove ${selectedItemsAmount} ${descriptor}?`)) {
      const deleteCalls = selectedStreamIds.map((streamId) => fetch('DELETE', qualifyUrl(ApiRoutes.StreamsApiController.delete(streamId).url)).then(() => streamId));

      Promise.allSettled(deleteCalls).then((result) => {
        const fulfilledRequests = result.filter((response) => response.status === 'fulfilled') as Array<{ status: 'fulfilled', value: string }>;
        const deletedStreamIds = fulfilledRequests.map(({ value }) => value);

        if (deletedStreamIds?.length !== selectedStreamIds.length) {
          const notDeletedStreamIds = selectedStreamIds.filter((streamId) => !deletedStreamIds.includes(streamId));
          const rejectedRequests = result.filter((response) => response.status === 'rejected') as Array<{ status: 'rejected', reason: FetchError }>;
          const errorMessages = uniq(rejectedRequests.map((request) => request.reason.responseMessage));

          if (notDeletedStreamIds.length !== selectedStreamIds.length) {
            queryClient.invalidateQueries(['streams', 'overview']);
          }

          setSelectedStreamIds(notDeletedStreamIds);

          UserNotification.error(`${notDeletedStreamIds.length} out of ${selectedItemsAmount} selected ${descriptor} could not be deleted. Status: ${errorMessages.join()}`);

          return;
        }

        queryClient.invalidateQueries(['streams', 'overview']);
        UserNotification.success(`${selectedItemsAmount} ${descriptor} ${StringUtils.pluralize(selectedItemsAmount, 'was', 'were')} deleted successfully.`, 'Success');
      });
    }
  }, [descriptor, queryClient, selectedItemsAmount, selectedStreamIds, setSelectedStreamIds]);

  return (
    <Button bsSize="xsmall" bsStyle="danger" onClick={onDelete}>Delete</Button>
  );
};

export default BulkActions;
