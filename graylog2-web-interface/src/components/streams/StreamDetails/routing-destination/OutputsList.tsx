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
import { useQueryClient } from '@tanstack/react-query';

import { Table } from 'components/bootstrap';
import { OutputsStore, type Output } from 'stores/outputs/OutputsStore';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import type { ConfigurationFormData } from 'components/configurationforms';
import type { AvailableOutputRequestedConfiguration } from 'components/streams/useAvailableOutputTypes';

import OutputItem from './OutputItem';

type Props = {
  outputs: Array<Output>,
  streamId: string,
  getTypeDefinition: (type: string) => AvailableOutputRequestedConfiguration,
  isLoadingOutputTypes: boolean,
}

const OutputsList = ({ outputs, streamId, getTypeDefinition, isLoadingOutputTypes }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const queryClient = useQueryClient();

  const handleUpdate = (output: Output, data: ConfigurationFormData<Output['configuration']>) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_UPDATED, {
      app_pathname: 'stream',
      app_action_value: 'create-output',
    });

    OutputsStore.update(output, data, (result) => {
      queryClient.invalidateQueries(['outputs', 'overview']);

      return result;
    });
  };

  return (
    <Table condensed striped hover>
      <thead>
        <tr>
          <th colSpan={2}>Name</th>
        </tr>
      </thead>
      <tbody>
        {outputs.map((output) => (
          <OutputItem key={output.id}
                      output={output}
                      streamId={streamId}
                      onUpdate={handleUpdate}
                      isLoadingOutputTypes={isLoadingOutputTypes}
                      getTypeDefinition={getTypeDefinition} />
        ))}

        {(outputs.length <= 0) && (
        <tr>
          <td colSpan={2}>No output defined.</td>
        </tr>
        )}
      </tbody>
    </Table>

  );
};

export default OutputsList;
