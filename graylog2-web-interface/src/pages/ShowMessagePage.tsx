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

import DocumentTitle from 'components/common/DocumentTitle';
import Spinner from 'components/common/Spinner';
import { Col, Row } from 'components/bootstrap';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import MessageDetail from 'views/components/messagelist/MessageDetail';
import withParams from 'routing/withParams';
import type { Stream } from 'views/stores/StreamsStore';
import type { Input } from 'components/messageloaders/Types';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import type { Message } from 'views/components/messagelist/Types';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import WindowDimensionsContextProvider from 'contexts/WindowDimensionsContextProvider';
import StreamsStore from 'stores/streams/StreamsStore';
import { InputsActions } from 'stores/inputs/InputsStore';
import { MessagesActions } from 'stores/messages/MessagesStore';
import { NodesActions } from 'stores/nodes/NodesStore';

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
        const streamsMap = newStreams.reduce((prev, stream) => ({ ...prev, [stream.id]: stream }), {});

        setStreams(Immutable.Map(streamsMap));
        setAllStreams(Immutable.List(newStreams));
      }
    });
  }, [setStreams, setAllStreams]);

  return { streams, allStreams };
};

const useMessage = (index: string, messageId: string) => {
  const [message, setMessage] = useState<Message | undefined>();
  const [inputs, setInputs] = useState<Immutable.Map<string, Input>>(Immutable.Map());

  useEffect(() => {
    const fetchData = async () => {
      const _message = await MessagesActions.loadMessage(index, messageId);
      setMessage(_message);

      if (_message.source_input_id) {
        const input = await InputsActions.get(_message.source_input_id);

        if (input) {
          const newInputs = Immutable.Map({ [input.id]: input });

          setInputs(newInputs);
        }
      }
    };

    fetchData();
  }, [index, messageId, setMessage, setInputs]);

  return { message, inputs };
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
  const { message, inputs } = useMessage(index, messageId);

  useEffect(() => { NodesActions.list(); }, []);

  const isLoaded = useMemo(() => (message !== undefined
    && streams !== undefined
    && inputs !== undefined
    && allStreams !== undefined), [message, streams, inputs, allStreams]);

  if (isLoaded) {
    const { streams: messageStreams, timestamp } = message.fields as MessageFields;
    const fieldTypesStreams = messageStreams.filter((streamId) => streams.has(streamId));

    return (
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
