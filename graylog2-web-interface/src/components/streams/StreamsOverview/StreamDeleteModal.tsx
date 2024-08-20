import * as React from 'react';
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import usePluginEntities from 'hooks/usePluginEntities';
import { ConfirmDialog } from 'components/common';

type Props = {
  onDelete: () => void,
  streamId: string,
  streamTitle: string,
  onCancel: () => void,
};

const useStreamDataWarehouseHasData = (streamId: string, enabled: boolean) => {
  const { fetchStreamDataWarehouse } = usePluginEntities('dataWarehouse')[0] ?? {};
  const { data: dataWarehouse, isError, isLoading } = useQuery(['stream', 'data-warehouse', streamId],
    () => fetchStreamDataWarehouse(streamId),
    { enabled: fetchStreamDataWarehouse && enabled },
  );

  return (isLoading || isError) ? undefined : (
    dataWarehouse?.message_count > 1 || dataWarehouse?.restore_history?.length > 0
  );
};

const useIsStreamDataWarehouseEnabled = (streamId: string, enabled: boolean) => {
  const { fetchStreamDataWarehouseStatus } = usePluginEntities('dataWarehouse')[0] ?? {};
  const { data: status, isError, isLoading } = useQuery(['data-warehouse-config', streamId, 'enabled'],
    () => fetchStreamDataWarehouseStatus(streamId),
    { enabled: fetchStreamDataWarehouseStatus && enabled },
  );

  return (isLoading || isError) ? undefined : status?.enabled;
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
