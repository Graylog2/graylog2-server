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
import { uniq } from 'lodash';
import * as React from 'react';
import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import ApiRoutes from 'routing/ApiRoutes';
import type FetchError from 'logic/errors/FetchError';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { Button } from 'components/bootstrap';
import StringUtils from 'util/StringUtils';

type Props = {
  selectedDefintions: Array<string>,
  refetchEventDefinitions: () => void,
  setSelectedEventDefinitionsIds: (definitionIds: Array<string>) => void
};

const BulkActions = ({ selectedDefintions, refetchEventDefinitions, setSelectedEventDefinitionsIds }: Props) => {
  const queryClient = useQueryClient();
  const selectedItemsAmount = selectedDefintions?.length;
  const descriptor = StringUtils.pluralize(selectedItemsAmount, 'event definition', 'event definitions');
  const onDelete = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to remove ${selectedItemsAmount} ${descriptor}?`)) {
      const deleteCalls = selectedDefintions.map((eventDefinitionId) => fetch('DELETE', qualifyUrl(ApiRoutes.EventDefinitionsApiController.delete(eventDefinitionId).url)).then(() => eventDefinitionId));

      Promise.allSettled(deleteCalls).then((result) => {
        const fulfilledRequests = result.filter((response) => response.status === 'fulfilled') as Array<{ status: 'fulfilled', value: string }>;
        const deletedEventDefinitionIds = fulfilledRequests.map(({ value }) => value);
        const notDeletedEventDefinitionIds = selectedDefintions?.filter((streamId) => !deletedEventDefinitionIds.includes(streamId));

        if (notDeletedEventDefinitionIds.length) {
          const rejectedRequests = result.filter((response) => response.status === 'rejected') as Array<{ status: 'rejected', reason: FetchError }>;
          const errorMessages = uniq(rejectedRequests.map((request) => request.reason.responseMessage));

          if (notDeletedEventDefinitionIds.length !== selectedDefintions.length) {
            queryClient.invalidateQueries(['eventDefinition', 'overview']);
          }

          UserNotification.error(`${notDeletedEventDefinitionIds.length} out of ${selectedDefintions} selected ${descriptor} could not be deleted. Status: ${errorMessages.join()}`);

          return;
        }

        queryClient.invalidateQueries(['eventDefinition', 'overview']);
        setSelectedEventDefinitionsIds(notDeletedEventDefinitionIds);
        refetchEventDefinitions();
        UserNotification.success(`${selectedItemsAmount} ${descriptor} ${StringUtils.pluralize(selectedItemsAmount, 'was', 'were')} deleted successfully.`, 'Success');
      });
    }
  }, [descriptor, queryClient, refetchEventDefinitions, selectedDefintions, selectedItemsAmount, setSelectedEventDefinitionsIds]);

  return (
    <Button bsSize="xsmall" bsStyle="danger" onClick={onDelete}>Delete</Button>
  );
};

export default BulkActions;
