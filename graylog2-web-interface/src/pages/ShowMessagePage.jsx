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

const NodesActions = ActionsProvider.getActions('Nodes');
const InputsActions = ActionsProvider.getActions('Inputs');
const MessagesActions = ActionsProvider.getActions('Messages');
const StreamsStore = StoreProvider.getStore('Streams');

type Props = {
  params: {
    index: string,
    messageId: string,
  },
};
const ShowMessagePage = ({ params: { index, messageId }, params }: Props) => {
  const [message, setMessage] = useState();
  const [inputs, setInputs] = useState(Immutable.Map);
  const [streams, setStreams] = useState();
  const [allStreams, setAllStreams] = useState();
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
      <DocumentTitle title={`Message ${params.messageId} on ${params.index}`}>
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
  params: PropTypes.shape({
    index: PropTypes.string.isRequired,
    messageId: PropTypes.string.isRequired,
  }).isRequired,
};

export default ShowMessagePage;
