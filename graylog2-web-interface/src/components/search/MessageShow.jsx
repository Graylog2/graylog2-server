import React from 'react';
import Immutable from 'immutable';
import MessageDetail from './MessageDetail';

const MessageShow = React.createClass({
  propTypes: {
    message: React.PropTypes.object,
    inputs: React.PropTypes.object,
    streams: React.PropTypes.object,
    nodes: React.PropTypes.object,
  },

  getInitialState() {
    return this._getImmutableProps(this.props);
  },

  componentWillReceiveProps(nextProps) {
    this.setState(this._getImmutableProps(nextProps));
  },

  _getImmutableProps(props) {
    return {
      streams: props.streams ? Immutable.Map(props.streams) : props.streams,
      nodes: props.nodes ? Immutable.Map(props.nodes) : props.nodes,
    };
  },

  possiblyHighlight(fieldName) {
    // No highlighting for the message details view.
    return this.props.message.fields[fieldName];
  },
  render() {
    return (
      <div className="row content">
        <div className="col-md-12">
          <MessageDetail {...this.props} message={this.props.message}
                                         inputs={this.props.inputs}
                                         streams={this.state.streams}
                                         nodes={this.state.nodes}
                                         possiblyHighlight={this.possiblyHighlight}
                                         showTimestamp/>
        </div>
      </div>
    );
  },
});

export default MessageShow;
