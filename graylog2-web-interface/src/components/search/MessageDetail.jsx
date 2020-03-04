import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';
import { Link } from 'react-router';

import { Button, ButtonGroup, Row, Col, Label } from 'components/graylog';
import { Icon, ClipboardButton, Timestamp } from 'components/common';

import StreamLink from 'components/streams/StreamLink';
import MessageFields from 'components/search/MessageFields';
import MessageDetailsTitle from 'components/search/MessageDetailsTitle';

import Routes from 'routing/Routes';

class MessageDetail extends React.Component {
  static propTypes = {
    renderForDisplay: PropTypes.func,
    inputs: PropTypes.object,
    nodes: PropTypes.object,
    message: PropTypes.object,
    streams: PropTypes.object,
    customFieldActions: PropTypes.node,
    searchConfig: PropTypes.object,
    disableMessageActions: PropTypes.bool,
  };

  state = {
    allStreamsLoaded: false,
    allStreams: Immutable.List(),
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
          <span style={{ wordBreak: 'break-word' }}>{node.short_node_id}</span>&nbsp;/&nbsp;<span style={{ wordBreak: 'break-word' }}>{node.hostname}
          </span>
        </a>
      );
    } else {
      nodeInformation = <span style={{ wordBreak: 'break-word' }}>stopped node</span>;
    }
    return nodeInformation;
  };

  _formatMessageActions = () => {
    if (this.props.disableMessageActions) {
      return <ButtonGroup className="pull-right" bsSize="small" />;
    }

    const messageUrl = this.props.message.index ? Routes.message_show(this.props.message.index, this.props.message.id) : '#';

    let showChanges = null;
    if (this.props.message.decoration_stats) {
      showChanges = <Button onClick={this._toggleShowOriginal} active={this.state.showOriginal}>Show changes</Button>;
    }

    return (
      <ButtonGroup className="pull-right" bsSize="small">
        {showChanges}
        <Button href={messageUrl}>Permalink</Button>

        <ClipboardButton title="Copy ID" text={this.props.message.id} />
      </ButtonGroup>
    );
  };

  _toggleShowOriginal = () => {
    this.setState({ showOriginal: !this.state.showOriginal });
  };

  render() {
    const streamIds = Immutable.Set(this.props.message.stream_ids);
    const streams = streamIds.map((id) => {
      const stream = this.props.streams.get(id);
      if (stream !== undefined) {
        return <li key={stream.id}><StreamLink stream={stream} /></li>;
      }
      return null;
    });

    // Legacy
    let viaRadio = this.props.message.source_radio_id;
    if (viaRadio) {
      viaRadio = (
        <span>
          via <em>{this._inputName(this.props.message.source_radio_input_id)}</em> on
          radio {this._nodeName(this.props.message.source_radio_id)}
        </span>
      );
    }

    let timestamp = [];
    const rawTimestamp = this.props.message.fields.timestamp;

    timestamp.push(<dt key={`dt-${rawTimestamp}`}>Timestamp</dt>);
    timestamp.push(<dd key={`dd-${rawTimestamp}`}><Timestamp dateTime={rawTimestamp} /></dd>);

    let receivedBy;
    if (this.props.message.source_input_id && this.props.message.source_node_id && this.props.nodes) {
      receivedBy = (
        <div>
          <dt>Received by</dt>
          <dd>
            <em>{this._inputName(this.props.message.source_input_id)}</em>{' '}
            on {this._nodeName(this.props.message.source_node_id)}
            { viaRadio && <br /> }
            {viaRadio}
          </dd>
        </div>
      );
    } else {
      receivedBy = null;
    }

    let messageTitle;
    if (this.props.message.index) {
      messageTitle = (
        <Link to={Routes.message_show(this.props.message.index, this.props.message.id)}>
          {this.props.message.id}
        </Link>
      );
    } else {
      messageTitle = <span>{this.props.message.id} <Label bsStyle="warning">Not stored</Label></span>;
    }

    return (
      <div>
        <Row className="row-sm">
          <Col md={12}>
            {this._formatMessageActions()}
            <MessageDetailsTitle>
              <Icon name="envelope" />
            &nbsp;
              {messageTitle}
            </MessageDetailsTitle>
          </Col>
        </Row>
        <Row>
          <Col md={3}>
            <dl className="message-details">
              {timestamp}
              {receivedBy}

              <dt>Stored in index</dt>
              <dd>{this.props.message.index ? this.props.message.index : 'Message is not stored'}</dd>

              { streamIds.size > 0 && <dt>Routed into streams</dt> }
              { streamIds.size > 0
            && (
            <dd className="stream-list">
              <ul>
                {streams}
              </ul>
            </dd>
            )
            }
            </dl>
          </Col>
          <Col md={9}>
            <div>
              <MessageFields message={this.props.message}
                             renderForDisplay={this.props.renderForDisplay}
                             customFieldActions={this.props.customFieldActions}
                             showDecoration={this.state.showOriginal} />
            </div>
          </Col>
        </Row>
      </div>
    );
  }
}

export default MessageDetail;
