// @flow strict
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
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

const ShowMessagePage = createReactClass({
  displayName: 'ShowMessagePage',

  propTypes: {
    params: PropTypes.shape({
      index: PropTypes.string.isRequired,
      messageId: PropTypes.string.isRequired,
    }).isRequired,
    searchConfig: PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      streams: Immutable.Map(),
      message: undefined,
      inputs: Immutable.Map(),
    };
  },

  componentDidMount() {
    const { params: { index, messageId } } = this.props;
    MessagesActions.loadMessage(index, messageId)
      .then((message) => {
        this.setState({ message });
        return message.source_input_id ? InputsActions.get(message.source_input_id) : Promise.resolve();
      })
      .then(this._formatInput);
    StreamsStore.listStreams().then((streams) => {
      const streamsMap = {};
      if (streams) {
        streams.forEach((stream) => {
          streamsMap[stream.id] = stream;
        });
        this.setState({ streams: Immutable.Map(streamsMap) });
      }
    });
    NodesActions.list();
  },

  _formatInput(input) {
    if (input) {
      const inputs = Immutable.Map({ [input.id]: input });
      this.setState({ inputs });
    }
  },

  _isLoaded() {
    const { message, streams, inputs } = this.state;
    return message && streams && inputs;
  },

  render() {
    if (this._isLoaded()) {
      const { params, searchConfig } = this.props;
      const { streams, inputs, message } = this.state;
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
  },
});

export default ShowMessagePage;
