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

import usePluginEntities from 'hooks/usePluginEntities';
import { ConfirmDialog } from 'components/common';
import useStreamDataLakeHasData from 'components/streams/StreamsOverview/hooks/useStreamDataLakeHasData';
import useIsStreamDataLakeEnabled from 'components/streams/StreamsOverview/hooks/useIsStreamDataLakeEnabled';

type Props = {
  onDelete: () => void;
  streamId: string;
  streamTitle: string;
  onCancel: () => void;
};

const StreamDeleteModal = ({ onDelete, streamId, streamTitle, onCancel }: Props) => {
  const DataLakeStreamDeleteWarning = usePluginEntities('dataLake')?.[0]?.DataLakeStreamDeleteWarning;
  const dataLakeState = useStreamDataLakeHasData(streamId, !!DataLakeStreamDeleteWarning);
  const isDataLakeEnabled = !!useIsStreamDataLakeEnabled(streamId, !!DataLakeStreamDeleteWarning);
  const hasArchivedData = dataLakeState?.hasArchivedData ?? false;
  const hasRetrievals = dataLakeState?.hasRetrievals ?? false;

  const shouldShowWarning = isDataLakeEnabled || hasArchivedData || hasRetrievals;

  return (
    <ConfirmDialog
      show
      onConfirm={onDelete}
      btnConfirmDisabled={shouldShowWarning}
      onCancel={onCancel}
      title="Delete Stream">
      {shouldShowWarning ? (
        <DataLakeStreamDeleteWarning
          streamId={streamId}
          isEnabled={isDataLakeEnabled}
          hasArchivedData={hasArchivedData}
          hasRetrievals={hasRetrievals}
        />
      ) : (
        `Do you really want to remove stream:  ${streamTitle}?`
      )}
    </ConfirmDialog>
  );
};

export default StreamDeleteModal;
