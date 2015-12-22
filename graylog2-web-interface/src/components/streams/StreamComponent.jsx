import React from 'react';
import { Col } from 'react-bootstrap';

import StreamsStore from 'stores/streams/StreamsStore';
import StreamRulesStore from 'stores/streams/StreamRulesStore';

import UserNotification from 'util/UserNotification';

import StreamList from './StreamList';
import Spinner from 'components/common/Spinner';

const StreamComponent = React.createClass({
  propTypes: {
    currentUser: React.PropTypes.object,
  },
  getInitialState() {
    return {};
  },
  componentDidMount() {
    this.loadData();
    StreamRulesStore.types().then((types) => {
      this.setState({streamRuleTypes: types});
    });
    StreamsStore.onChange(this.loadData);
    StreamRulesStore.onChange(this.loadData);
  },
  loadData() {
    StreamsStore.load((streams) => {
      this.setState({streams: streams});
    });
  },
  _onSave(streamId, stream) {
    StreamsStore.save(stream, () => {
      UserNotification.success('Stream has been successfully created.', 'Success');
    });
  },
  _isLoading() {
    return !(this.state.streams && this.state.streamRuleTypes);
  },
  render() {
    if (this._isLoading()) {
      return (
        <div style={{marginLeft: 10}}>
          <Spinner/>
        </div>
      );
    }

    return (
      <Col md={12}>
        <StreamList streams={this.state.streams} streamRuleTypes={this.state.streamRuleTypes}
                    permissions={this.props.currentUser.permissions} user={this.props.currentUser}
                    onStreamCreated={this._onSave}/>
      </Col>
    );
  },
});

module.exports = StreamComponent;
