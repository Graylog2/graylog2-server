import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';
import { Link } from 'react-router';

import { Button, ButtonGroup, Col, Label, Row } from 'components/graylog';
import { ClipboardButton, Icon, Timestamp } from 'components/common';

import StreamLink from 'components/streams/StreamLink';
import MessageFields from 'components/search/MessageFields';
import MessageDetailsTitle from 'components/search/MessageDetailsTitle';

import Routes from 'routing/Routes';

class MessageDetail extends React.Component {
  static propTypes = {
    renderForDisplay: PropTypes.func.isRequired,
    inputs: PropTypes.object,
    nodes: PropTypes.object,
    message: PropTypes.object.isRequired,
    streams: PropTypes.object.isRequired,
    customFieldActions: PropTypes.node,
  };

  static defaultProps = {
    inputs: undefined,
    nodes: undefined,
    customFieldActions: undefined,
  };

  _inputName = (inputId) => {
    const { inputs } = this.props;
    const input = inputs.get(inputId);
    return input ? <span style={{ wordBreak: 'break-word' }}>{input.title}</span> : 'deleted input';
  };

  _nodeName = (nodeId) => {
    const { nodes } = this.props;
    const node = nodes.get(nodeId);
    let nodeInformation;

    if (node) {
      const nodeURL = Routes.node(nodeId);
      nodeInformation = (
        <a href={nodeURL}>
          <Icon name="code-fork" />
          &nbsp;
          <span style={{ wordBreak: 'break-word' }}>{node.short_node_id}</span>&nbsp;/&nbsp;
          <span style={{ wordBreak: 'break-word' }}>{node.hostname}</span>
        </a>
      );
    } else {
      nodeInformation = <span style={{ wordBreak: 'break-word' }}>stopped node</span>;
    }
    return nodeInformation;
  };

  _formatMessageActions = () => {
    const { message, customFieldActions } = this.props;
    if (!customFieldActions) {
      return <ButtonGroup className="pull-right" bsSize="small" />;
    }

    const messageUrl = message.index ? Routes.message_show(message.index, message.id) : '#';

    return (
      <ButtonGroup className="pull-right" bsSize="small">
        <Button href={messageUrl}>Permalink</Button>

        <ClipboardButton title="Copy ID" text={message.id} />
      </ButtonGroup>
    );
  };

  render() {
    const { renderForDisplay, nodes, message, customFieldActions } = this.props;
    const streamIds = Immutable.Set(message.stream_ids);
    const streams = streamIds.map((id) => {
      // eslint-disable-next-line react/destructuring-assignment
      const stream = this.props.streams.get(id);
      if (stream !== undefined) {
        return <li key={stream.id}><StreamLink stream={stream} /></li>;
      }
      return null;
    });

    // Legacy
    let viaRadio = message.source_radio_id;
    if (viaRadio) {
      viaRadio = (
        <span>
          via <em>{this._inputName(message.source_radio_input_id)}</em> on
          radio {this._nodeName(message.source_radio_id)}
        </span>
      );
    }

    let timestamp = [];
    const rawTimestamp = message.fields.timestamp;

    timestamp.push(<dt key={`dt-${rawTimestamp}`}>Timestamp</dt>);
    timestamp.push(<dd key={`dd-${rawTimestamp}`}><Timestamp dateTime={rawTimestamp} /></dd>);

    let receivedBy;
    if (message.source_input_id && message.source_node_id && nodes) {
      receivedBy = (
        <div>
          <dt>Received by</dt>
          <dd>
            <em>{this._inputName(message.source_input_id)}</em>{' '}
            on {this._nodeName(message.source_node_id)}
            { viaRadio && <br /> }
            {viaRadio}
          </dd>
        </div>
      );
    } else {
      receivedBy = null;
    }

    let messageTitle;
    if (message.index) {
      messageTitle = (
        <Link to={Routes.message_show(message.index, message.id)}>
          {message.id}
        </Link>
      );
    } else {
      messageTitle = <span>{message.id} <Label bsStyle="warning">Not stored</Label></span>;
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
              <dd>{message.index ? message.index : 'Message is not stored'}</dd>

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
              <MessageFields message={message}
                             renderForDisplay={renderForDisplay}
                             customFieldActions={customFieldActions} />
            </div>
          </Col>
        </Row>
      </div>
    );
  }
}

export default MessageDetail;
