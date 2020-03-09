import PropTypes from 'prop-types';
import React from 'react';
import { Col, Row } from 'components/graylog';
import Immutable from 'immutable';
import StringUtils from 'util/StringUtils';
import MessageDetail from './MessageDetail';

class MessageShow extends React.Component {
  static propTypes = {
    message: PropTypes.object.isRequired,
    inputs: PropTypes.object,
    streams: PropTypes.object.isRequired,
    nodes: PropTypes.object,
  };

  static defaultProps = {
    inputs: undefined,
    nodes: undefined,
  };

  constructor(props) {
    super(props);

    this.state = this._getImmutableProps(props);
  }

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
    const { message } = this.props;
    return StringUtils.stringify(message.fields[fieldName]);
  };

  render() {
    const { inputs, message } = this.props;
    const { streams, nodes } = this.state;
    return (
      <Row className="content">
        <Col md={12}>
          <MessageDetail {...this.props}
                         message={message}
                         inputs={inputs}
                         streams={streams}
                         nodes={nodes}
                         renderForDisplay={this.renderForDisplay}
                         showTimestamp />
        </Col>
      </Row>
    );
  }
}

export default MessageShow;
