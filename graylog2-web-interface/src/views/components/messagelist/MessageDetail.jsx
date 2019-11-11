import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';
import { Link } from 'react-router';

import { Row, Col, Label } from 'components/graylog';
import StreamLink from 'components/streams/StreamLink';
import { MessageFields } from 'views/components/messagelist';
import { Spinner, Timestamp, Icon } from 'components/common';

import Routes from 'routing/Routes';
import MessageActions from './MessageActions';
import MessageMetadata from './MessageMetadata';

class MessageDetail extends React.Component {
  static propTypes = {
    allStreams: PropTypes.object,
    disableFieldActions: PropTypes.bool,
    disableMessageActions: PropTypes.bool,
    disableSurroundingSearch: PropTypes.bool,
    disableTestAgainstStream: PropTypes.bool,
    expandAllRenderAsync: PropTypes.bool,
    fields: PropTypes.object.isRequired,
    inputs: PropTypes.object,
    message: PropTypes.object,
    nodes: PropTypes.object,
    searchConfig: PropTypes.object,
    showTimestamp: PropTypes.bool,
    streams: PropTypes.object,
  };

  static defaultProps = {
    allStreams: {},
    disableFieldActions: false,
    disableMessageActions: false,
    disableSurroundingSearch: false,
    disableTestAgainstStream: false,
    expandAllRenderAsync: false,
    inputs: {},
    message: {},
    nodes: {},
    searchConfig: {},
    showTimestamp: true,
    streams: {},
  };

  state = {
    showOriginal: false,
  };

  _inputName = (inputId) => {
    const input = this.props.inputs.get(inputId);
    return input ? <span style={{ wordBreak: 'break-word' }}>{input.title}</span> : 'deleted input';
  };

  _nodeName = (nodeId) => {
    const node = this.props.nodes.get(nodeId);
    let nodeInformation;

    if (node) {
      const nodeURL = Routes.node(nodeId);
      nodeInformation = (
        <a href={nodeURL}>
          <Icon name="code-fork" />
          &nbsp;
          <span style={{ wordBreak: 'break-word' }}>{node.short_node_id}</span>&nbsp;/&nbsp;<span style={{ wordBreak: 'break-word' }}>{node.hostname}</span>
        </a>
      );
    } else {
      nodeInformation = <span style={{ wordBreak: 'break-word' }}>stopped node</span>;
    }
    return nodeInformation;
  };

  _toggleShowOriginal = () => {
    this.setState(({ showOriginal }) => ({ showOriginal: !showOriginal }));
  };

  _formatReceivedBy = (sourceNodeId, sourceInputId) => {
    if (!sourceNodeId) {
      return null;
    }

    return (
      <div>
        <dt>Received by</dt>
        <dd>
          <em>{this._inputName(sourceInputId)}</em>{' '}
          on {this._nodeName(sourceNodeId)}
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
    const { expandAllRenderAsync, message, allStreams } = this.props;
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

    const streamIds = Immutable.Set(fields.streams);
    const streams = streamIds.map((streamId) => {
      const stream = this.props.streams.get(streamId);
      if (stream !== undefined) {
        return <li key={stream.id}><StreamLink stream={stream} /></li>;
      }
      return null;
    });

    let timestamp = null;
    if (this.props.showTimestamp) {
      timestamp = [];
      const rawTimestamp = fields.timestamp;

      timestamp.push(<dt key={`dt-${rawTimestamp}`}>Timestamp</dt>);
      timestamp.push(<dd key={`dd-${rawTimestamp}`}><Timestamp dateTime={rawTimestamp} /></dd>);
    }

    // eslint-disable-next-line camelcase
    const { gl2_source_node, gl2_source_input } = fields;
    const receivedBy = this._formatReceivedBy(gl2_source_node, gl2_source_input);

    const messageTitle = this._formatMessageTitle(index, id);

    return (
      <React.Fragment>
        <Row className="row-sm">
          <Col md={12}>
            <MessageActions index={index}
                            id={id}
                            fields={fields}
                            decorationStats={message.decoration_stats}
                            disabled={this.props.disableMessageActions}
                            disableSurroundingSearch={this.props.disableSurroundingSearch}
                            disableTestAgainstStream={this.props.disableTestAgainstStream}
                            showOriginal={this.state.showOriginal}
                            toggleShowOriginal={this._toggleShowOriginal}
                            searchConfig={this.props.searchConfig}
                            streams={allStreams} />
            <h3 className="message-details-title">
              <Icon name="envelope" />
              &nbsp;
              {messageTitle}
            </h3>
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
                           fields={this.props.fields}
                           disableFieldActions={this.props.disableFieldActions}
                           showDecoration={this.state.showOriginal} />
          </Col>
        </Row>
      </React.Fragment>
    );
  }
}

export default MessageDetail;
