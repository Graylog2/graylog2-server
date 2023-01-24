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
