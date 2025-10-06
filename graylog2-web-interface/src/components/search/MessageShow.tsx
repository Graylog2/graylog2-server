/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import Immutable from 'immutable';

import { Col, Row } from 'components/bootstrap';
import StringUtils from 'util/StringUtils';
import type { Stream } from 'logic/streams/types';

import MessageDetail from './MessageDetail';

type MessageShowProps = Omit<React.ComponentProps<typeof MessageDetail>, 'renderForDisplay'> & {
  message: any;
  inputs?: any;
  streams?: Immutable.Map<string, Stream>;
  nodes?: any;
};

const getImmutableProps = (props: MessageShowProps) => ({
  streams: props.streams ? Immutable.Map(props.streams) : props.streams,
});

class MessageShow extends React.Component<
  MessageShowProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    inputs: undefined,
    nodes: undefined,
    streams: undefined,
  };

  constructor(props: MessageShowProps) {
    super(props);

    this.state = getImmutableProps(props);
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState(getImmutableProps(nextProps));
  }

  renderForDisplay = (fieldName) => {
    // No highlighting for the message details view.
    const { message } = this.props;

    return <>{StringUtils.stringify(message.fields[fieldName])}</>;
  };

  render() {
    const { inputs, message } = this.props;
    const { streams } = this.state;

    return (
      <Row className="content">
        <Col md={12}>
          <MessageDetail
            {...this.props}
            message={message}
            inputs={inputs}
            streams={streams}
            renderForDisplay={this.renderForDisplay}
          />
        </Col>
      </Row>
    );
  }
}

export default MessageShow;
