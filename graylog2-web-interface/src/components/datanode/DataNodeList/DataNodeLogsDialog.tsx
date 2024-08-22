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
import styled from 'styled-components';

import { BootstrapModalWrapper, Modal, SegmentedControl } from 'components/bootstrap';

import useDataNodeLogs from '../hooks/useDataNodeLogs';

const LogsContainer = styled.div`
  word-break: break-all;
  overflow-wrap: break-word;
  white-space: pre-wrap;
  max-height: 500px;

  & td {
    min-width: 64px;
    vertical-align: text-top;
    padding-bottom: 4px;
  }
`;

type LogsType = 'stdout' | 'stderr';

const LogsTypeSegments: Array<{value: LogsType, label: string}> = [
  { value: 'stdout', label: 'STDOUT' },
  { value: 'stderr', label: 'STDERR' },
];

type Props = {
  show: boolean,
  hostname: string,
  onHide: () => void,
};

const DataNodeLogsDialog = ({ show, hostname, onHide }: Props) => {
  const [logsType, setLogsType] = useState<LogsType>('stdout');

  const logs = useDataNodeLogs(hostname, show && !!hostname);

  return (
    <BootstrapModalWrapper showModal={show}
                           onHide={onHide}
                           bsSize="large"
                           backdrop>
      <Modal.Header closeButton>
        <Modal.Title>{hostname} logs</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <SegmentedControl<LogsType> data={LogsTypeSegments}
                                    value={logsType}
                                    onChange={setLogsType} />
        <pre>
          {logs[logsType] ? (
            <LogsContainer>
              <table>
                {/* eslint-disable-next-line react/no-array-index-key */}
                <tbody>{logs[logsType]?.map((log, key) => (<tr key={key}><td>{log}</td></tr>))}</tbody>
              </table>
            </LogsContainer>
          ) : ('No logs.')}
        </pre>
      </Modal.Body>
    </BootstrapModalWrapper>
  );
};

export default DataNodeLogsDialog;
