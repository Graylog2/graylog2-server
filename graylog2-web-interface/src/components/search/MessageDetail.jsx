import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';
import { LinkContainer } from 'react-router-bootstrap';
import { Link } from 'react-router';

import { Button, ButtonGroup, Row, Col, DropdownButton, MenuItem, Label } from 'components/graylog';
import { Icon, Spinner, ClipboardButton, Timestamp } from 'components/common';
import StoreProvider from 'injection/StoreProvider';

import StreamLink from 'components/streams/StreamLink';
import MessageFields from 'components/search/MessageFields';

import SurroundingSearchButton from 'components/search/SurroundingSearchButton';

import Routes from 'routing/Routes';

const StreamsStore = StoreProvider.getStore('Streams');

class MessageDetail extends React.Component {
  static propTypes = {
    allStreams: PropTypes.object,
    allStreamsLoaded: PropTypes.bool,
    disableTestAgainstStream: PropTypes.bool,
    disableSurroundingSearch: PropTypes.bool,
    expandAllRenderAsync: PropTypes.bool,
    showTimestamp: PropTypes.bool,
    disableFieldActions: PropTypes.bool,
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

  componentDidMount() {
    if (this.props.allStreams === undefined) {
      // our parent does not provide allStreams for the test against stream menu, we have to load it ourselves
      // this can happen if the component is used outside the regular search result
      // only load the streams per page
      if (this.state.allStreamsLoaded || this.props.disableTestAgainstStream) {
        return;
      }
      const promise = StreamsStore.listStreams();
      promise.done(streams => this._onStreamsLoaded(streams));
    }
  }

  _onStreamsLoaded = (streams) => {
    this.setState({ allStreamsLoaded: true, allStreams: Immutable.List(streams).sortBy(stream => stream.title) });
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
        { (!streamList && !this.props.allStreamsLoaded) && (
        <MenuItem header><Icon name="spinner" spin />
          Loading streams
        </MenuItem>
        ) }
        { (!streamList && this.props.allStreamsLoaded) && <MenuItem header>No streams available</MenuItem> }
      </DropdownButton>
    );
  };

  _formatMessageActions = () => {
    if (this.props.disableMessageActions) {
      return <ButtonGroup className="pull-right" bsSize="small" />;
    }

    const messageUrl = this.props.message.index ? Routes.message_show(this.props.message.index, this.props.message.id) : '#';

    let surroundingSearchButton;
    if (!this.props.disableSurroundingSearch) {
      surroundingSearchButton = (
        <SurroundingSearchButton id={this.props.message.id}
                                 timestamp={this.props.message.timestamp}
                                 searchConfig={this.props.searchConfig}
                                 messageFields={this.props.message.fields} />
      );
    }

    let showChanges = null;
    if (this.props.message.decoration_stats) {
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

  render() {
    // Short circuit when all messages are being expanded at the same time
    if (this.props.expandAllRenderAsync) {
      return (
        <Row>
          <Col md={12}>
            <Spinner />
          </Col>
        </Row>
      );
    }

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

    let timestamp = null;
    if (this.props.showTimestamp) {
      timestamp = [];
      const rawTimestamp = this.props.message.fields.timestamp;

      timestamp.push(<dt key={`dt-${rawTimestamp}`}>Timestamp</dt>);
      timestamp.push(<dd key={`dd-${rawTimestamp}`}><Timestamp dateTime={rawTimestamp} /></dd>);
    }

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
            <h3 className="message-details-title">
              <Icon name="envelope" />
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
                             disableFieldActions={this.props.disableFieldActions}
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
