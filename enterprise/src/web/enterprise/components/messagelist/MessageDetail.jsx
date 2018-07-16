import PropTypes from 'prop-types';
import React from 'react';
import { ButtonGroup, Button, Row, Col, DropdownButton, MenuItem, Label } from 'react-bootstrap';
import Immutable from 'immutable';
import { Link } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';

import StreamLink from 'components/streams/StreamLink';
import { MessageFields } from 'enterprise/components/messagelist';
import { Spinner, ClipboardButton, Timestamp } from 'components/common';
import SurroundingSearchButton from 'components/search/SurroundingSearchButton';

import Routes from 'routing/Routes';

class MessageDetail extends React.Component {
  static propTypes = {
    allStreams: PropTypes.object,
    allStreamsLoaded: PropTypes.bool,
    customFieldActions: PropTypes.node,
    disableFieldActions: PropTypes.bool,
    disableMessageActions: PropTypes.bool,
    disableSurroundingSearch: PropTypes.bool,
    disableTestAgainstStream: PropTypes.bool,
    expandAllRenderAsync: PropTypes.bool,
    fields: PropTypes.object.isRequired,
    inputs: PropTypes.object,
    message: PropTypes.object,
    nodes: PropTypes.object,
    possiblyHighlight: PropTypes.func,
    searchConfig: PropTypes.object,
    showTimestamp: PropTypes.bool,
    streams: PropTypes.object,
  };

  static defaultProps = {
    allStreams: {},
    allStreamsLoaded: true,
    customFieldActions: null,
    disableFieldActions: false,
    disableMessageActions: false,
    disableSurroundingSearch: false,
    disableTestAgainstStream: false,
    expandAllRenderAsync: false,
    inputs: {},
    message: {},
    nodes: {},
    possiblyHighlight: () => {},
    searchConfig: {},
    showTimestamp: true,
    streams: {},
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
          <i className="fa fa-code-fork" />
          &nbsp;
          <span style={{ wordBreak: 'break-word' }}>{node.short_node_id}</span>&nbsp;/&nbsp;<span
          style={{ wordBreak: 'break-word' }}>{node.hostname}</span>
        </a>
      );
    } else {
      nodeInformation = <span style={{ wordBreak: 'break-word' }}>stopped node</span>;
    }
    return nodeInformation;
  };

  _getAllStreams = () => {
    if (this.props.allStreams) {
      return this.props.allStreams;
    }
    return this.state.allStreams;
  };

  _getTestAgainstStreamButton = () => {
    if (this.props.disableTestAgainstStream) {
      return null;
    }

    let streamList = null;
    this._getAllStreams().forEach((stream) => {
      if (!streamList) {
        streamList = [];
      }
      if (stream.is_default) {
        streamList.push(
          <MenuItem key={stream.id} disabled title="Cannot test against the default stream">{stream.title}</MenuItem>,
        );
      } else {
        streamList.push(
          <LinkContainer key={stream.id}
                         to={Routes.stream_edit_example(stream.id, this.props.message.index,
                                                        this.props.message.id)}>
            <MenuItem>{stream.title}</MenuItem>
          </LinkContainer>,
        );
      }
    });

    return (
      <DropdownButton pullRight
                      bsSize="small"
                      title="Test against stream"
                      id="select-stream-dropdown">
        { streamList }
        { (!streamList && !this.props.allStreamsLoaded) && <MenuItem header><i className="fa fa-spin fa-spinner" />
          Loading streams</MenuItem> }
        { (!streamList && this.props.allStreamsLoaded) && <MenuItem header>No streams available</MenuItem> }
      </DropdownButton>
    );
  };

  // eslint-disable-next-line camelcase
  _formatMessageActions = ({ index, id, fields, decoration_stats }) => {
    if (this.props.disableMessageActions) {
      return <ButtonGroup className="pull-right" bsSize="small" />;
    }

    const messageUrl = index ? Routes.message_show(index, id) : '#';

    let surroundingSearchButton;
    if (!this.props.disableSurroundingSearch) {
      surroundingSearchButton = (
        <SurroundingSearchButton id={id}
                                 timestamp={fields.timestamp}
                                 searchConfig={this.props.searchConfig}
                                 messageFields={fields} />
      );
    }

    let showChanges = null;
    // eslint-disable-next-line camelcase
    if (decoration_stats) {
      showChanges = <Button onClick={this._toggleShowOriginal} active={this.state.showOriginal}>Show changes</Button>;
    }

    return (
      <ButtonGroup className="pull-right" bsSize="small">
        {showChanges}
        <Button href={messageUrl}>Permalink</Button>

        <ClipboardButton title="Copy ID" text={this.props.message.id} />
        {surroundingSearchButton}
        {this._getTestAgainstStreamButton()}
      </ButtonGroup>
    );
  };

  _toggleShowOriginal = () => {
    this.setState({ showOriginal: !this.state.showOriginal });
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
    const { expandAllRenderAsync, message } = this.props;
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
      <div>
        <Row className="row-sm">
          <Col md={12}>
            {this._formatMessageActions(message)}
            <h3 className="message-details-title">
              <i className="fa fa-envelope" />
              &nbsp;
              {messageTitle}
            </h3>
          </Col>
        </Row>
        <Row>
          <Col md={3}>
            <dl className="message-details">
              {timestamp}
              {receivedBy}

              <dt>Stored in index</dt>
              <dd>{index || 'Message is not stored'}</dd>

              {streamIds.size > 0 && <dt>Routed into streams</dt>}
              {streamIds.size > 0 &&
              <dd className="stream-list">
                <ul>
                  {streams}
                </ul>
              </dd>
              }
            </dl>
          </Col>
          <Col md={9}>
            <div>
              <MessageFields message={message}
                             fields={this.props.fields}
                             possiblyHighlight={this.props.possiblyHighlight}
                             disableFieldActions={this.props.disableFieldActions}
                             customFieldActions={this.props.customFieldActions}
                             showDecoration={this.state.showOriginal}
              />
            </div>
          </Col>
        </Row>
      </div>
    );
  }
}

export default MessageDetail;
