import PropTypes from 'prop-types';
import React from 'react';
import { Col, Row } from 'react-bootstrap';
import Immutable from 'immutable';
import MessageDetail from './MessageDetail';
import StringUtils from 'util/StringUtils';

class MessageShow extends React.Component {
  static propTypes = {
    message: PropTypes.object,
    inputs: PropTypes.object,
    streams: PropTypes.object,
    nodes: PropTypes.object,
  };

  componentWillReceiveProps(nextProps) {
    this.setState(this._getImmutableProps(nextProps));
  }

  _getImmutableProps = (props) => {
    return {
      streams: props.streams ? Immutable.Map(props.streams) : props.streams,
      nodes: props.nodes ? Immutable.Map(props.nodes) : props.nodes,
    };
  };

  renderForDisplay = (fieldName) => {
    // No highlighting for the message details view.
    return StringUtils.stringify(this.props.message.fields[fieldName]);
  };

  state = this._getImmutableProps(this.props);

  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <MessageDetail {...this.props} message={this.props.message}
                                         inputs={this.props.inputs}
                                         streams={this.state.streams}
                                         nodes={this.state.nodes}
                                         renderForDisplay={this.renderForDisplay}
                                         showTimestamp />
        </Col>
      </Row>
    );
  }
}

export default MessageShow;
