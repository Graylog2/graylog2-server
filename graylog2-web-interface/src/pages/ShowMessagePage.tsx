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
// @flow strict
import PropTypes from 'prop-types';
import React, { useEffect, useMemo, useState } from 'react';
import * as Immutable from 'immutable';

import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';
import DocumentTitle from 'components/common/DocumentTitle';
import Spinner from 'components/common/Spinner';
import { Col, Row } from 'components/graylog';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import MessageDetail from 'views/components/messagelist/MessageDetail';
import withParams from 'routing/withParams';

const NodesActions = ActionsProvider.getActions('Nodes');
const InputsActions = ActionsProvider.getActions('Inputs');
const MessagesActions = ActionsProvider.getActions('Messages');
const StreamsStore = StoreProvider.getStore('Streams');

type Props = {
  params: {
    index: string | undefined | null,
    messageId: string | undefined | null,
  },
};

const ShowMessagePage = ({ params: { index, messageId } }: Props) => {
  if (!index || !messageId) {
    throw new Error('index and messageId need to be specified!');
  }

  const [message, setMessage] = useState();
  const [inputs, setInputs] = useState(Immutable.Map);
  const [streams, setStreams] = useState<Immutable.Map<string, unknown>>();
  const [allStreams, setAllStreams] = useState<Immutable.List<unknown>>();

  useEffect(() => { NodesActions.list(); }, []);

  useEffect(() => {
    MessagesActions.loadMessage(index, messageId)
      .then((_message) => {
        setMessage(_message);

        return _message.source_input_id ? InputsActions.get(_message.source_input_id) : Promise.resolve();
      })
      .then((input) => {
        if (input) {
          const newInputs = Immutable.Map({ [input.id]: input });

          setInputs(newInputs);
        }
      });
  }, [index, messageId, setMessage, setInputs]);

  useEffect(() => {
    StreamsStore.listStreams().then((newStreams) => {
      if (newStreams) {
        const streamsMap = newStreams.reduce((prev, stream) => ({ ...prev, [stream.id]: stream }), {});

        setStreams(Immutable.Map(streamsMap));
        setAllStreams(Immutable.List(newStreams));
      }
    });
  }, [setStreams, setAllStreams]);

  const isLoaded = useMemo(() => (message !== undefined
    && streams !== undefined
    && inputs !== undefined
    && allStreams !== undefined), [message, streams, inputs, allStreams]);

  if (isLoaded) {
    return (
      <DocumentTitle title={`Message ${messageId} on ${index}`}>
        <Row className="content">
          <Col md={12}>
            <InteractiveContext.Provider value={false}>
              <MessageDetail fields={Immutable.Map()}
                             streams={streams}
                             allStreams={allStreams}
                             disableSurroundingSearch
                             disableFieldActions
                             inputs={inputs}
                             message={message} />
            </InteractiveContext.Provider>
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
