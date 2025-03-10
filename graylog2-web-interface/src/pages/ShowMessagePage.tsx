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
import React, { useEffect, useMemo, useState, useContext } from 'react';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import useMessage from 'views/hooks/useMessage';
import DocumentTitle from 'components/common/DocumentTitle';
import Spinner from 'components/common/Spinner';
import { Col, Row } from 'components/bootstrap';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import MessageDetail from 'views/components/messagelist/MessageDetail';
import withParams from 'routing/withParams';
import type { Input } from 'components/messageloaders/Types';
import WindowDimensionsContextProvider from 'contexts/WindowDimensionsContextProvider';
import { InputsActions } from 'stores/inputs/InputsStore';
import { NodesActions } from 'stores/nodes/NodesStore';
import { isLocalNode } from 'views/hooks/useIsLocalNode';
import ViewsStoreProvider from 'views/stores/ViewsStoreProvider';
import View from 'views/logic/views/View';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import SingleMessageFieldTypesProvider from 'views/components/fieldtypes/SingleMessageFieldTypesProvider';
import StreamsContext from 'contexts/StreamsContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type { Message } from 'views/components/messagelist/Types';
import ErrorPage from 'components/errors/ErrorPage';

type Props = {
  params: {
    index: string | undefined | null;
    messageId: string | undefined | null;
  };
};

const useInputs = (sourceInputId: string | undefined, gl2SourceNode: string | undefined) => {
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

type MessageFields = {
  streams: Array<string>;
  timestamp: string;
};

type ShowMessagePageProps = {
  message: Message;
  messageId: string;
  index: string;
};
const ShowMessagePage = ({ message, messageId, index }: ShowMessagePageProps) => {
  const streams = useContext(StreamsContext);
  const streamsMap = Immutable.Map(Object.fromEntries(streams.map((stream) => [stream.id, stream])));
  const streamsList = Immutable.List(streams);
  const inputs = useInputs(message?.source_input_id, message?.fields.gl2_source_node);

  useEffect(() => {
    NodesActions.list();
  }, []);

  const isLoaded = useMemo(() => message !== undefined && inputs !== undefined, [message, inputs]);

  const view = useMemo(() => View.create(), []);
  const executionState = useMemo(() => SearchExecutionState.empty(), []);

  if (isLoaded) {
    const { streams: messageStreams, timestamp } = message.fields as MessageFields;
    const fieldTypesStreams = messageStreams.filter((streamId) => streamsMap.has(streamId));

    return (
      <ViewsStoreProvider view={view} initialQuery="none" isNew={false} executionState={executionState}>
        <DocumentTitle title={`Message ${messageId} on ${index}`}>
          <Row className="content" id="sticky-augmentations-container">
            <Col md={12}>
              <WindowDimensionsContextProvider>
                <SingleMessageFieldTypesProvider streams={fieldTypesStreams} timestamp={timestamp}>
                  <FieldTypesContext.Consumer>
                    {({ all }) => (
                      <InteractiveContext.Provider value={false}>
                        <MessageDetail
                          fields={all}
                          streams={streamsMap}
                          allStreams={streamsList}
                          disableSurroundingSearch
                          inputs={inputs}
                          message={message}
                        />
                      </InteractiveContext.Provider>
                    )}
                  </FieldTypesContext.Consumer>
                </SingleMessageFieldTypesProvider>
              </WindowDimensionsContextProvider>
            </Col>
          </Row>
        </DocumentTitle>
      </ViewsStoreProvider>
    );
  }

  return <Spinner data-testid="spinner" />;
};

const ErrorPre = styled.pre`
  margin: 0 3rem;
`;
const ShowMessagePageLoader = ({ params: { index, messageId } }: Props) => {
  if (!index || !messageId) {
    throw new Error('index and messageId need to be specified!');
  }
  const { data: message, isError, error } = useMessage(index, messageId);

  return isError ? (
    <ErrorPage
      title="Index/Message not found"
      description="The index or message specified was not found. The most probable reason for this is that the index containing the
      message referenced has been deleted. The full error message is:">
      <ErrorPre>{error.details}</ErrorPre>
    </ErrorPage>
  ) : (
    <ShowMessagePage message={message} messageId={messageId} index={index} />
  );
};

export default withParams(ShowMessagePageLoader);
