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
import { useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import { ConfirmDialog } from 'components/common';
import useStreamDataWarehouseHasData from 'components/streams/StreamsOverview/hooks/useStreamDataWarehouseHasData';
import useIsStreamDataWarehouseEnabled from 'components/streams/StreamsOverview/hooks/useIsStreamDataWarehouseEnabled';

type Props = {
  onDelete: () => void,
  streamId: string,
  streamTitle: string,
  onCancel: () => void,
};

const StreamDeleteModal = ({ onDelete, streamId, streamTitle, onCancel }: Props) => {
  const DataWarehouseStreamDeleteWarning = usePluginEntities('dataWarehouse')?.[0]?.DataWarehouseStreamDeleteWarning;
  const streamDataWarehouseHasData = useStreamDataWarehouseHasData(streamId, !!DataWarehouseStreamDeleteWarning);
  const isDataWarehouseEnable = useIsStreamDataWarehouseEnabled(streamId, !!DataWarehouseStreamDeleteWarning);

  const shouldShowWarning = useMemo(() => isDataWarehouseEnable || streamDataWarehouseHasData, [isDataWarehouseEnable, streamDataWarehouseHasData]);

  return (
    <ConfirmDialog show
                   onConfirm={onDelete}
                   btnConfirmDisabled={shouldShowWarning}
                   onCancel={onCancel}
                   title="Delete Stream">
      {shouldShowWarning ? <DataWarehouseStreamDeleteWarning /> : `Do you really want to remove stream:  ${streamTitle}?`}
    </ConfirmDialog>
  );
};

export default StreamDeleteModal;
