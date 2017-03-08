import React from 'react';
import { Col, Row } from 'react-bootstrap';
import Immutable from 'immutable';
import MessageDetail from './MessageDetail';
import StringUtils from 'util/StringUtils';

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
    return StringUtils.stringify(this.props.message.fields[fieldName]);
  },
  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <MessageDetail {...this.props} message={this.props.message}
                                         inputs={this.props.inputs}
                                         streams={this.state.streams}
                                         nodes={this.state.nodes}
                                         possiblyHighlight={this.possiblyHighlight}
                                         showTimestamp />
        </Col>
      </Row>
    );
  },
});

export default MessageShow;
