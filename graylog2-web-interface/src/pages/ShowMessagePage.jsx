// @flow strict
import PropTypes from 'prop-types';
import React, { useEffect, useMemo, useState } from 'react';
import * as Immutable from 'immutable';

import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';
import DocumentTitle from 'components/common/DocumentTitle';
import Spinner from 'components/common/Spinner';
import connect from 'stores/connect';
import { Col, Row } from 'components/graylog';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import MessageDetail from 'views/components/messagelist/MessageDetail';

const NodesActions = ActionsProvider.getActions('Nodes');
const InputsActions = ActionsProvider.getActions('Inputs');
const MessagesActions = ActionsProvider.getActions('Messages');
const NodesStore = StoreProvider.getStore('Nodes');
const StreamsStore = StoreProvider.getStore('Streams');

const ConnectedMessageDetail = connect(MessageDetail, { nodes: NodesStore }, ({ nodes }) => ({ nodes: Immutable.Map(nodes.nodes) }));

const ShowMessagePage = ({ params: { index, messageId }, params, searchConfig, nodes }) => {
  const [message, setMessage] = useState();
  const [inputs, setInputs] = useState();
  const [streams, setStreams] = useState();
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
  }, [index, messageId]);
  useEffect(() => {
    StreamsStore.listStreams().then((newStreams) => {
      if (newStreams) {
        const streamsMap = newStreams.reduce((prev, stream) => ({ ...prev, [stream.id]: stream }), {});
        setStreams(Immutable.Map(streamsMap));
      }
    });
  }, []);
  const isLoaded = useMemo(() => (message && streams && inputs), [message, streams, inputs]);

  if (isLoaded) {
    return (
      <DocumentTitle title={`Message ${params.messageId} on ${params.index}`}>
        <Row className="content">
          <Col md={12}>
            <InteractiveContext.Provider value={false}>
              <ConnectedMessageDetail fields={Immutable.Map()}
                                      streams={streams}
                                      disableSurroundingSearch
                                      disableMessageActions
                                      disableFieldActions
                                      inputs={inputs}
                                      message={message}
                                      searchConfig={searchConfig} />
            </InteractiveContext.Provider>
          </Col>
        </Row>
      </DocumentTitle>
    );
  }
  return <Spinner />;
};

ShowMessagePage.propTypes = {
  params: PropTypes.shape({
    index: PropTypes.string.isRequired,
    messageId: PropTypes.string.isRequired,
  }).isRequired,
    searchConfig: PropTypes.object.isRequired,
};

export default connect(ShowMessagePage, { nodes: NodesStore });
