import React from 'react';
import Immutable from 'immutable';

import { Col, Row } from 'components/bootstrap';
import StringUtils from 'util/StringUtils';

import MessageDetail from './MessageDetail';

const getImmutableProps = (props) => ({
  streams: props.streams ? Immutable.Map(props.streams) : props.streams,
});

type MessageShowProps = {
  message: any;
  inputs?: any;
  streams?: any;
  nodes?: any;
};

class MessageShow extends React.Component<MessageShowProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    inputs: undefined,
    nodes: undefined,
    streams: undefined,
  };

  constructor(props) {
    super(props);

    this.state = getImmutableProps(props);
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState(getImmutableProps(nextProps));
  }

  renderForDisplay = (fieldName) => {
    // No highlighting for the message details view.
    const { message } = this.props;

    return StringUtils.stringify(message.fields[fieldName]);
  };

  render() {
    const { inputs, message } = this.props;
    const { streams } = this.state;

    return (
      <Row className="content">
        <Col md={12}>
          <MessageDetail {...this.props}
                         message={message}
                         inputs={inputs}
                         streams={streams}
                         renderForDisplay={this.renderForDisplay}
                         showTimestamp />
        </Col>
      </Row>
    );
  }
}

export default MessageShow;
