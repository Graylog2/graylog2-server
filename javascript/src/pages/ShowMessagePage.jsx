import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import Immutable from 'immutable';
import MessageShow from 'components/search/MessageShow';
import Spinner from 'components/common/Spinner';

import StreamsStore from 'stores/streams/StreamsStore';
import NodesActions from 'actions/nodes/NodesActions';
import NodesStore from 'stores/nodes/NodesStore';
import InputsStore from 'stores/inputs/InputsStore';
import MessagesStore from 'stores/messages/MessagesStore';

const ShowMessagePage = React.createClass({
  mixins: [Reflux.connect(NodesStore), Reflux.ListenerMethods],
  propTypes: {
    params: PropTypes.object,
  },
  getInitialState() {
    return {
      streams: undefined,
      inputs: undefined,
      message: undefined,
    };
  },
  componentDidMount() {
    MessagesStore.loadMessage(this.props.params.index, this.props.params.messageId).then(message => this.setState({message: message}));
    StreamsStore.listStreams().then(streams => this.setState({streams: Immutable.Map(streams)}));
    InputsStore.list(inputs => this.setState({inputs: Immutable.Map(inputs)}));
    NodesActions.list.triggerPromise();
  },
  _isLoaded() {
    return this.state.message && this.state.streams && this.state.nodes && this.state.inputs;
  },
  render() {
    if (this._isLoaded()) {
      return (
        <MessageShow message={this.state.message} inputs={this.state.inputs} nodes={Immutable.Map(this.state.nodes)}
                     streams={this.state.streams} allStreamsLoaded/>
      );
    } else {
      return <Spinner/>;
    }
  },
});

export default ShowMessagePage;
