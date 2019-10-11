import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Immutable from 'immutable';
import MessageShow from 'components/search/MessageShow';
import DocumentTitle from 'components/common/DocumentTitle';
import Spinner from 'components/common/Spinner';

import ActionsProvider from 'injection/ActionsProvider';

import StoreProvider from 'injection/StoreProvider';
import connect from 'stores/connect';

const NodesActions = ActionsProvider.getActions('Nodes');
const InputsActions = ActionsProvider.getActions('Inputs');
const MessagesActions = ActionsProvider.getActions('Messages');
const NodesStore = StoreProvider.getStore('Nodes');
const StreamsStore = StoreProvider.getStore('Streams');

const ConnectedMessageShow = connect(MessageShow, { nodes: NodesStore }, ({ nodes }) => ({ nodes: Immutable.Map(nodes.nodes) }));

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
          <ConnectedMessageShow message={message}
                                inputs={inputs}
                                streams={streams}
                                allStreamsLoaded
                                searchConfig={searchConfig} />
        </DocumentTitle>
      );
    }
    return <Spinner />;
  },
});

export default ShowMessagePage;
