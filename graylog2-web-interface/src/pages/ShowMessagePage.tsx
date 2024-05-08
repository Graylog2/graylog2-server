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
import PropTypes from 'prop-types';
import React, { useEffect, useMemo, useState } from 'react';
import * as Immutable from 'immutable';

import useMessage from 'views/hooks/useMessage';
import DocumentTitle from 'components/common/DocumentTitle';
import Spinner from 'components/common/Spinner';
import { Col, Row } from 'components/bootstrap';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import MessageDetail from 'views/components/messagelist/MessageDetail';
import withParams from 'routing/withParams';
import type { Stream } from 'views/stores/StreamsStore';
import type { Input } from 'components/messageloaders/Types';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import WindowDimensionsContextProvider from 'contexts/WindowDimensionsContextProvider';
import StreamsStore from 'stores/streams/StreamsStore';
import { InputsActions } from 'stores/inputs/InputsStore';
import { NodesActions } from 'stores/nodes/NodesStore';
import { isLocalNode } from 'views/hooks/useIsLocalNode';
import PluggableStoreProvider from 'components/PluggableStoreProvider';
import View from 'views/logic/views/View';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

type Props = {
  params: {
    index: string | undefined | null,
    messageId: string | undefined | null,
  },
};

const useStreams = () => {
  const [streams, setStreams] = useState<Immutable.Map<string, Stream>>();
  const [allStreams, setAllStreams] = useState<Immutable.List<Stream>>();

  useEffect(() => {
    StreamsStore.listStreams().then((newStreams) => {
      if (newStreams) {
        const streamsMap = Object.fromEntries(newStreams.map((stream) => [stream.id, stream]));

        setStreams(Immutable.Map(streamsMap));
        setAllStreams(Immutable.List(newStreams));
      }
    });
  }, [setStreams, setAllStreams]);

  return { streams, allStreams };
};

const useInputs = (sourceInputId, gl2SourceNode) => {
  const [inputs, setInputs] = useState<Immutable.Map<string, Input>>(Immutable.Map());

  useEffect(() => {
    const fetchInputs = async () => {
      if (sourceInputId && (await isLocalNode(gl2SourceNode))) {
        const input = await InputsActions.get(sourceInputId);

        if (input) {
          const newInputs = Immutable.Map({ [input.id]: input });

          setInputs(newInputs);
        }
      }
    };

    fetchInputs();
  }, [setInputs, sourceInputId, gl2SourceNode]);

  return inputs;
};

type FieldTypesProviderProps = {
  children: React.ReactNode,
  streams: Array<string>,
  timestamp: string,
};

const FieldTypesProvider = ({ streams, timestamp, children }: FieldTypesProviderProps) => {
  const { data: fieldTypes } = useFieldTypes(streams, { type: 'absolute', from: timestamp, to: timestamp });
  const types = useMemo(() => {
    const fieldTypesList = Immutable.List(fieldTypes);

    return ({ all: fieldTypesList, queryFields: Immutable.Map({ query: fieldTypesList }) });
  }, [fieldTypes]);

  return (
    <FieldTypesContext.Provider value={types}>
      {children}
    </FieldTypesContext.Provider>
  );
};

type MessageFields = {
  streams: Array<string>,
  timestamp: string,
};

const ShowMessagePage = ({ params: { index, messageId } }: Props) => {
  if (!index || !messageId) {
    throw new Error('index and messageId need to be specified!');
  }

  const { streams, allStreams } = useStreams();
  const { data: message } = useMessage(messageId, index);
  const inputs = useInputs(message?.source_input_id, message?.fields.gl2_source_node);

  useEffect(() => { NodesActions.list(); }, []);

  const isLoaded = useMemo(() => (message !== undefined
    && streams !== undefined
    && inputs !== undefined
    && allStreams !== undefined), [message, streams, inputs, allStreams]);

  const view = useMemo(() => View.create(), []);
  const executionState = useMemo(() => SearchExecutionState.empty(), []);

  if (isLoaded) {
    const { streams: messageStreams, timestamp } = message.fields as MessageFields;
    const fieldTypesStreams = messageStreams.filter((streamId) => streams.has(streamId));

    return (
      <PluggableStoreProvider view={view} initialQuery="none" isNew={false} executionState={executionState}>
        <DocumentTitle title={`Message ${messageId} on ${index}`}>
          <Row className="content" id="sticky-augmentations-container">
            <Col md={12}>
              <WindowDimensionsContextProvider>
                <FieldTypesProvider streams={fieldTypesStreams} timestamp={timestamp}>
                  <InteractiveContext.Provider value={false}>
                    <MessageDetail fields={Immutable.List()}
                                   streams={streams}
                                   allStreams={allStreams}
                                   disableSurroundingSearch
                                   inputs={inputs}
                                   message={message} />
                  </InteractiveContext.Provider>
                </FieldTypesProvider>
              </WindowDimensionsContextProvider>
            </Col>
          </Row>
        </DocumentTitle>
      </PluggableStoreProvider>
    );
  }

  return <Spinner data-testid="spinner" />;
};

ShowMessagePage.propTypes = {
  params: PropTypes.exact({
    index: PropTypes.string.isRequired,
    messageId: PropTypes.string.isRequired,
  }).isRequired,
};

export default withParams(ShowMessagePage);
