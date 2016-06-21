import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import Immutable from 'immutable';
import MessageShow from 'components/search/MessageShow';
import Spinner from 'components/common/Spinner';

import ActionsProvider from 'injection/ActionsProvider';
const NodesActions = ActionsProvider.getActions('Nodes');
const InputsActions = ActionsProvider.getActions('Inputs');
const MessagesActions = ActionsProvider.getActions('Messages');

import StoreProvider from 'injection/StoreProvider';
const NodesStore = StoreProvider.getStore('Nodes');
const StreamsStore = StoreProvider.getStore('Streams');
const InputsStore = StoreProvider.getStore('Inputs');
// eslint-disable-next-line no-unused-vars
const MessagesStore = StoreProvider.getStore('Messages');

const ShowMessagePage = React.createClass({
  propTypes: {
    params: PropTypes.object,
    searchConfig: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(NodesStore), Reflux.listenTo(InputsStore, '_formatInput')],
  getInitialState() {
    return {
      streams: undefined,
      inputs: undefined,
      message: undefined,
    };
  },
  componentDidMount() {
    MessagesActions.loadMessage.triggerPromise(this.props.params.index, this.props.params.messageId).then(message => {
      this.setState({ message: message });
      InputsActions.get.triggerPromise(message.source_input_id);
    });
    StreamsStore.listStreams().then(streams => {
      const streamsMap = {};
      streams.forEach((stream) => {
        streamsMap[stream.id] = stream;
      });
      this.setState({ streams: Immutable.Map(streamsMap) });
    });
    NodesActions.list.triggerPromise();
  },
  _formatInput(state) {
    const input = {};
    input[state.input.id] = state.input;
    this.setState({ inputs: Immutable.Map(input) });
  },
  _isLoaded() {
    return this.state.message && this.state.streams && this.state.nodes && this.state.inputs;
  },
  render() {
    if (this._isLoaded()) {
      return (
        <MessageShow message={this.state.message} inputs={this.state.inputs} nodes={Immutable.Map(this.state.nodes)}
                     streams={this.state.streams} allStreamsLoaded searchConfig={this.props.searchConfig} />
      );
    } else {
      return <Spinner />;
    }
  },
});

export default ShowMessagePage;
