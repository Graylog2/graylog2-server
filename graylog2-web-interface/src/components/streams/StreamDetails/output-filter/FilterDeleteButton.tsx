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
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { Button } from 'components/bootstrap';
import { ConfirmDialog, Icon } from 'components/common';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import useStreamOutputRuleMutation from 'components/streams/hooks/useStreamOutputRuleMutation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type Props ={
  streamId: string,
  filterOutputRule: StreamOutputFilterRule,
};

const FilterDeleteButton = ({ streamId, filterOutputRule }: Props) => {
  const [showDialog, setShowDialog] = useState(false);
  const { removeStreamOutputRule } = useStreamOutputRuleMutation();
  const queryClient = useQueryClient();
  const sendTelemetry = useSendTelemetry();

  const onConfirmDelete = async () => {
    await removeStreamOutputRule({ streamId, filterId: filterOutputRule.id }).then(() => {
      queryClient.invalidateQueries(['streams']);
    });

    setShowDialog(false);
  };

  const onDelete = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_DATA_ROUTING_FILTER_DELETE_OPENED, {
      app_pathname: 'streams',
    });

    setShowDialog(true);
  };

  return (
    <>
      <Button bsStyle="danger"
              bsSize="xsmall"
              onClick={onDelete}
              title="View">
        <Icon name="delete" type="regular" />
      </Button>
      {showDialog && (
      <ConfirmDialog title="Delete Rule"
                     show
                     onConfirm={onConfirmDelete}
                     onCancel={() => setShowDialog(false)}>
        {`Are you sure you want to delete  ${filterOutputRule.title} rule ?`}
      </ConfirmDialog>
      )}
    </>
  );
};

export default FilterDeleteButton;
