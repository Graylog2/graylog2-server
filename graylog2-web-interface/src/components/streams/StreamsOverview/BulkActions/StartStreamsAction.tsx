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
import { useCallback } from 'react';
import * as React from 'react';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import MenuItem from 'components/bootstrap/MenuItem';
import UserNotification from 'util/UserNotification';

type Props = {
  descriptor: string,
  handleFailures: (failures: Array<{ entity_id: string }>, actionPastTense: string) => void,
  onSelect?: () => void,
  refetchStreams: () => void,
  selectedStreamIds: Array<string>,
}

const StartStreamsActions = ({ selectedStreamIds, handleFailures, refetchStreams, descriptor, onSelect }: Props) => {
  const onStartStreams = useCallback(() => {
    if (typeof onSelect === 'function') {
      onSelect();
    }

    fetch(
      'POST',
      qualifyUrl(ApiRoutes.StreamsApiController.bulk_resume().url),
      { entity_ids: selectedStreamIds },
    ).then(({ failures }) => handleFailures(failures, 'started'))
      .catch((error) => {
        UserNotification.error(`An error occurred while starting streams. ${error}`);
      })
      .finally(() => {
        refetchStreams();
      });
  }, [handleFailures, onSelect, refetchStreams, selectedStreamIds]);

  return (
    <MenuItem onSelect={onStartStreams}>Start {descriptor}</MenuItem>
  );
};

StartStreamsActions.defaultProps = {
  onSelect: undefined,
};

export default StartStreamsActions;
