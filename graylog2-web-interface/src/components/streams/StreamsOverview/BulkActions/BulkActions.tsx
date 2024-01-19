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
import { useQueryClient } from '@tanstack/react-query';

import StringUtils from 'util/StringUtils';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import MenuItem from 'components/bootstrap/MenuItem';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import StopStreamsAction from 'components/streams/StreamsOverview/BulkActions/StopStreamsAction';
import SearchStreamsAction from 'components/streams/StreamsOverview/BulkActions/SearchStreamsAction';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import IfPermitted from 'components/common/IfPermitted';

import StartStreamsAction from './StartStreamsAction';
import AssignIndexSetModal from './AssignIndexSetModal';

type Props = {
  indexSets: Array<IndexSet>
}

const BulkActions = ({ indexSets }: Props) => {
  const queryClient = useQueryClient();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const [showIndexSetModal, setShowIndexSetModal] = useState(false);
  const selectedItemsAmount = selectedEntities?.length;
  const descriptor = StringUtils.pluralize(selectedItemsAmount, 'stream', 'streams');
  const refetchStreams = useCallback(() => queryClient.invalidateQueries(['streams', 'overview']), [queryClient]);

  const toggleAssignIndexSetModal = useCallback(() => {
    setShowIndexSetModal((cur) => !cur);
  }, []);

  const handleFailures = useCallback((failures: Array<{ entity_id: string }>, actionPastTense: string) => {
    if (failures?.length) {
      const notDeletedStreamIds = failures.map(({ entity_id }) => entity_id);
      setSelectedEntities(notDeletedStreamIds);
      UserNotification.error(`${notDeletedStreamIds.length} out of ${selectedItemsAmount} selected ${descriptor} could not be ${actionPastTense}.`);
    } else {
      setSelectedEntities([]);
      UserNotification.success(`${selectedItemsAmount} ${descriptor} ${StringUtils.pluralize(selectedItemsAmount, 'was', 'were')} ${actionPastTense} successfully.`, 'Success');
    }
  }, [descriptor, selectedItemsAmount, setSelectedEntities]);

  const onDelete = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to remove ${selectedItemsAmount} ${descriptor}? This action cannot be undone.`)) {
      fetch(
        'POST',
        qualifyUrl(ApiRoutes.StreamsApiController.bulk_delete().url),
        { entity_ids: selectedEntities },
      ).then(({ failures }) => handleFailures(failures, 'deleted')).catch((error) => {
        UserNotification.error(`An error occurred while deleting streams. ${error}`);
      }).finally(() => {
        refetchStreams();
      });
    }
  }, [descriptor, handleFailures, refetchStreams, selectedItemsAmount, selectedEntities]);

  return (
    <>
      {showIndexSetModal && (
        <AssignIndexSetModal toggleShowModal={toggleAssignIndexSetModal}
                             indexSets={indexSets}
                             descriptor={descriptor}
                             refetchStreams={refetchStreams} />
      )}
      <BulkActionsDropdown>
        <IfPermitted permissions="indexsets:read">
          <MenuItem onSelect={toggleAssignIndexSetModal}>Assign index set</MenuItem>
        </IfPermitted>
        <SearchStreamsAction />
        <StartStreamsAction handleFailures={handleFailures}
                            refetchStreams={refetchStreams}
                            descriptor={descriptor} />

        <StopStreamsAction handleFailures={handleFailures}
                           refetchStreams={refetchStreams}
                           descriptor={descriptor} />

        <MenuItem onSelect={onDelete}>Delete</MenuItem>
      </BulkActionsDropdown>
    </>
  );
};

export default BulkActions;
