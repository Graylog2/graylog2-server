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
import React, { useMemo, useState, useCallback } from 'react';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Link, Select } from 'components/common';
import { Col, ControlLabel, FormGroup, HelpBlock, Panel, Row } from 'components/bootstrap';
import RawMessageLoader from 'components/messageloaders/RawMessageLoader';
import Routes from 'routing/Routes';
import fetch from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import MessageFormatter from 'logic/message/MessageFormatter';
import type { FormattedMessage } from 'logic/message/MessageFormatter';
import type { Stream } from 'logic/streams/types';
import type { Message } from 'views/components/messagelist/Types';

import SimulationResults from './SimulationResults';

const DEFAULT_STREAM_ID = '000000000000000000000001';

type SimulateResponse = {
  messages: Array<FormattedMessage>;
  [key: string]: unknown;
};

type ProcessorSimulatorProps = {
  streams: Stream[];
};

const getFormattedStreams = (streams: Stream[]) => {
  if (!streams) {
    return [];
  }

  return streams
    .map((stream) => ({ value: stream.id, label: stream.title }))
    .sort((s1, s2) => naturalSort(s1.label, s2.label));
};

type RawSimulateResponse = {
  messages: Array<unknown>;
  [key: string]: unknown;
};

const simulateMessage = (
  stream: Pick<Stream, 'id'>,
  messageFields: Record<string, unknown>,
  inputId: string,
): Promise<SimulateResponse> => {
  const url = URLUtils.qualifyUrl(ApiRoutes.SimulatorController.simulate().url);
  const simulation = {
    stream_id: stream.id,
    message: messageFields,
    input_id: inputId,
  };

  return fetch<RawSimulateResponse>('POST', url, simulation).then((response) => ({
    ...response,
    messages: response.messages.map((msg) => MessageFormatter.formatMessageSummary(msg)),
  }));
};

const ProcessorSimulator = ({ streams }: ProcessorSimulatorProps) => {
  const defaultStream = useMemo(() => streams.find((s) => s.id === DEFAULT_STREAM_ID) ?? streams[0], [streams]);

  const [stream, setStream] = useState<Stream>(defaultStream);
  const [message, setMessage] = useState<Message | undefined>(undefined);
  const [simulation, setSimulation] = useState<SimulateResponse | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(undefined);

  const onMessageLoad = useCallback(
    (loadedMessage: Message | undefined, options: { inputId?: string }) => {
      setMessage(loadedMessage);
      setSimulation(undefined);
      setLoading(true);
      setError(undefined);

      simulateMessage(stream, loadedMessage?.fields ?? {}, options.inputId ?? '').then(
        (response) => {
          setSimulation(response);
          setLoading(false);
        },
        (err: unknown) => {
          setLoading(false);
          setError(err);
        },
      );
    },
    [stream],
  );

  const onStreamSelect = useCallback(
    (selectedStream: string) => {
      const found = streams.find((s) => s.id.toLowerCase() === selectedStream.toLowerCase());

      setStream(found);
    },
    [streams],
  );

  if (streams.length === 0) {
    return (
      <div>
        <Row className="row-sm">
          <Col md={8} mdOffset={2}>
            <Panel bsStyle="danger" header="No streams found">
              Pipelines operate on streams, but your system currently has no streams. Please{' '}
              <Link to={Routes.STREAMS}>create a stream</Link> and come back here later to test pipelines processing
              messages in your new stream.
            </Panel>
          </Col>
        </Row>
      </div>
    );
  }

  const streamHelp = (
    <span>
      Select a stream to use during simulation, the <em>{defaultStream.title}</em> stream is used by default.
    </span>
  );

  return (
    <div>
      <Row>
        <Col md={12}>
          <h1>Load a message</h1>
          <p>
            Build an example message that will be used in the simulation.{' '}
            <strong>
              No real messages will be altered. All actions are purely simulated on the temporary input you provide
              below.
            </strong>
          </p>
          <Row className="row-sm">
            <Col md={7}>
              <FormGroup id="streamSelectorSimulation">
                <ControlLabel>Stream</ControlLabel>
                <Select
                  options={getFormattedStreams(streams)}
                  onChange={onStreamSelect}
                  value={stream.id}
                  required
                  clearable={false}
                />
                <HelpBlock>{streamHelp}</HelpBlock>
              </FormGroup>
            </Col>
          </Row>
          <RawMessageLoader onMessageLoaded={onMessageLoad} inputIdSelector />
        </Col>
      </Row>
      <SimulationResults
        stream={stream}
        originalMessage={message}
        simulationResults={simulation}
        isLoading={loading}
        error={error}
      />
    </div>
  );
};

export default ProcessorSimulator;
