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
import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Link } from 'components/graylog/router';
import { Col, Label, Row } from 'components/graylog';
import StreamLink from 'components/streams/StreamLink';
import { MessageFields } from 'views/components/messagelist';
import MessageDetailsTitle from 'components/search/MessageDetailsTitle';
import { Icon, Spinner, Timestamp } from 'components/common';
import Routes from 'routing/Routes';

import MessageActions from './MessageActions';
import MessageMetadata from './MessageMetadata';
import NodeName from './NodeName';
import { SearchesConfig } from 'components/search/SearchConfig';
import { Message } from 'views/components/messagelist/Types';
import { Input } from 'components/messageloaders/Types';
import { Stream } from 'views/stores/StreamsStore';
import ImmutablePropTypes from 'react-immutable-proptypes';
import CustomPropTypes from 'views/components/CustomPropTypes';
import { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

type Props = {
  allStreams?: Immutable.List<Stream>,
  disableMessageActions?: boolean,
  disableSurroundingSearch?: boolean,
  disableTestAgainstStream?: boolean,
  expandAllRenderAsync?: boolean,
  fields: FieldTypeMappingsList,
  inputs?: Immutable.Map<string, Input>,
  message?: Message,
  searchConfig?: SearchesConfig,
  showTimestamp?: boolean,
  streams?: Immutable.Map<string, Stream>,
};
type State = {
  showOriginal: boolean,
};

class MessageDetail extends React.Component<Props, State> {
  static propTypes = {
    allStreams: ImmutablePropTypes.list,
    disableFieldActions: PropTypes.bool,
    disableMessageActions: PropTypes.bool,
    disableSurroundingSearch: PropTypes.bool,
    disableTestAgainstStream: PropTypes.bool,
    expandAllRenderAsync: PropTypes.bool,
    fields: ImmutablePropTypes.list,
    inputs: ImmutablePropTypes.map,
    message: CustomPropTypes.Message,
    searchConfig: PropTypes.any,
    showTimestamp: PropTypes.bool,
    streams: ImmutablePropTypes.map,
  };

  static defaultProps: Partial<Props> = {
    allStreams: Immutable.List(),
    disableMessageActions: false,
    disableSurroundingSearch: false,
    disableTestAgainstStream: false,
    expandAllRenderAsync: false,
    inputs: Immutable.Map(),
    message: {} as Message,
    searchConfig: {} as SearchesConfig,
    showTimestamp: true,
    streams: Immutable.Map(),
  };

  constructor(props) {
    super(props);

    this.state = {
      showOriginal: false,
    };
  }

  _inputName = (inputId) => {
    const { inputs } = this.props;
    const input = inputs.get(inputId);

    return input ? <span style={{ wordBreak: 'break-word' }}>{input.title}</span> : 'deleted input';
  };

  _toggleShowOriginal = () => {
    this.setState(({ showOriginal }) => ({ showOriginal: !showOriginal }));
  };

  _formatReceivedBy = (sourceNodeId, sourceInputId) => {
    if (!sourceNodeId) {
      return null;
    }

    const forwarderPlugin = PluginStore.exports('forwarder');
    const ForwarderReceivedBy = forwarderPlugin?.[0]?.ForwarderReceivedBy;
    const isLocalNode = forwarderPlugin?.[0]?.isLocalNode;

    if (isLocalNode && !isLocalNode(sourceNodeId)) {
      return <ForwarderReceivedBy inputId={sourceInputId} forwarderNodeId={sourceNodeId} />;
    }

    return (
      <div>
        <dt>Received by</dt>
        <dd>
          <em>{this._inputName(sourceInputId)}</em>{' '}
          on <NodeName nodeId={sourceNodeId} />
        </dd>
      </div>
    );
  };

  _formatMessageTitle = (index, id) => {
    if (index) {
      return (
        <Link to={Routes.message_show(index, id)}>{id}</Link>
      );
    }

    return <span>{id} <Label bsStyle="warning">Not stored</Label></span>;
  };

  render() {
    const {
      expandAllRenderAsync,
      message,
      fields: messageFields,
      allStreams,
      disableMessageActions,
      disableSurroundingSearch,
      disableTestAgainstStream,
      searchConfig,
      showTimestamp,
    } = this.props;
    const { showOriginal } = this.state;
    const { fields, index, id } = message;

    // Short circuit when all messages are being expanded at the same time
    if (expandAllRenderAsync) {
      return (
        <Row>
          <Col md={12}>
            <Spinner />
          </Col>
        </Row>
      );
    }

    const streamIds = Immutable.Set(fields.streams as Array<string>);
    const streams = streamIds.map((streamId) => {
      // eslint-disable-next-line react/destructuring-assignment
      const stream = this.props.streams.get(streamId);

      if (stream !== undefined) {
        return <li key={stream.id}><StreamLink stream={stream} /></li>;
      }

      return null;
    }).toSet();

    let timestamp = null;

    if (showTimestamp) {
      timestamp = [];
      const rawTimestamp = fields.timestamp;

      timestamp.push(<dt key={`dt-${rawTimestamp}`}>Timestamp</dt>);
      timestamp.push(<dd key={`dd-${rawTimestamp}`}><Timestamp dateTime={rawTimestamp} /></dd>);
    }

    const { gl2_source_node, gl2_source_input } = fields;
    const receivedBy = this._formatReceivedBy(gl2_source_node, gl2_source_input);

    const messageTitle = this._formatMessageTitle(index, id);

    return (
      <>
        <Row className="row-sm">
          <Col md={12}>
            <MessageActions index={index}
                            id={id}
                            fields={fields}
                            decorationStats={message.decoration_stats}
                            disabled={disableMessageActions}
                            disableSurroundingSearch={disableSurroundingSearch}
                            disableTestAgainstStream={disableTestAgainstStream}
                            showOriginal={showOriginal}
                            toggleShowOriginal={this._toggleShowOriginal}
                            searchConfig={searchConfig}
                            streams={allStreams} />
            <MessageDetailsTitle>
              <Icon name="envelope" />
              &nbsp;
              {messageTitle}
            </MessageDetailsTitle>
          </Col>
        </Row>
        <Row>
          <Col md={3}>
            <MessageMetadata timestamp={timestamp}
                             index={index}
                             receivedBy={receivedBy}
                             streams={streams} />
          </Col>
          <Col md={9}>
            <MessageFields message={message}
                           fields={messageFields} />
          </Col>
        </Row>
      </>
    );
  }
}

export default MessageDetail;
