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
import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import StringUtils from 'util/StringUtils';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import MenuItem from 'components/bootstrap/MenuItem';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import AssignIndexSetAction from 'components/streams/StreamsOverview/BulkActions/AssignIndexSetAction';
import StopStreamsAction from 'components/streams/StreamsOverview/BulkActions/StopStreamsAction';
import SearchStreamsAction from 'components/streams/StreamsOverview/BulkActions/SearchStreamsAction';

import StartStreamsAction from './StartStreamsAction';

type Props = {
  selectedStreamIds: Array<string>,
  setSelectedStreamIds: (streamIds: Array<string>) => void,
  indexSets: Array<IndexSet>
}

const BulkActions = ({ selectedStreamIds, setSelectedStreamIds, indexSets }: Props) => {
  const queryClient = useQueryClient();

  const selectedItemsAmount = selectedStreamIds?.length;
  const descriptor = StringUtils.pluralize(selectedItemsAmount, 'stream', 'streams');
  const refetchStreams = useCallback(() => queryClient.invalidateQueries(['streams', 'overview']), [queryClient]);

  const handleFailures = useCallback((failures: Array<{ entity_id: string }>, actionPastTense: string) => {
    if (failures?.length) {
      const notDeletedStreamIds = failures.map(({ entity_id }) => entity_id);
      setSelectedStreamIds(notDeletedStreamIds);
      UserNotification.error(`${notDeletedStreamIds.length} out of ${selectedItemsAmount} selected ${descriptor} could not be ${actionPastTense}.`);
    } else {
      setSelectedStreamIds([]);
      UserNotification.success(`${selectedItemsAmount} ${descriptor} ${StringUtils.pluralize(selectedItemsAmount, 'was', 'were')} ${actionPastTense} successfully.`, 'Success');
    }
  }, [descriptor, selectedItemsAmount, setSelectedStreamIds]);

  const onDelete = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to remove ${selectedItemsAmount} ${descriptor}? This action cannot be undone.`)) {
      fetch(
        'POST',
        qualifyUrl(ApiRoutes.StreamsApiController.bulk_delete().url),
        { entity_ids: selectedStreamIds },
      ).then(({ failures }) => handleFailures(failures, 'deleted')).catch((error) => {
        UserNotification.error(`An error occurred while deleting streams. ${error}`);
      }).finally(() => {
        refetchStreams();
      });
    }
  }, [descriptor, handleFailures, refetchStreams, selectedItemsAmount, selectedStreamIds]);

  return (
    <BulkActionsDropdown selectedEntities={selectedStreamIds} setSelectedEntities={setSelectedStreamIds}>
      <AssignIndexSetAction indexSets={indexSets}
                            selectedStreamIds={selectedStreamIds}
                            setSelectedStreamIds={setSelectedStreamIds}
                            descriptor={descriptor}
                            refetchStreams={refetchStreams} />
      <SearchStreamsAction selectedStreamIds={selectedStreamIds} />
      <StartStreamsAction handleFailures={handleFailures}
                          selectedStreamIds={selectedStreamIds}
                          refetchStreams={refetchStreams}
                          descriptor={descriptor} />

      <StopStreamsAction handleFailures={handleFailures}
                         selectedStreamIds={selectedStreamIds}
                         refetchStreams={refetchStreams}
                         descriptor={descriptor} />

      <MenuItem onSelect={onDelete}>Delete</MenuItem>
    </BulkActionsDropdown>
  );
};

export default BulkActions;
