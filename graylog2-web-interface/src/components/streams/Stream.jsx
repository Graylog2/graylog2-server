import React, { PropTypes } from 'react';
import StreamThroughput from './StreamThroughput';
import StreamControls from './StreamControls';
import StreamStateBadge from './StreamStateBadge';
import CollapsibleStreamRuleList from 'components/streamrules/CollapsibleStreamRuleList';
import PermissionsMixin from 'util/PermissionsMixin';

import StoreProvider from 'injection/StoreProvider';
const StreamsStore = StoreProvider.getStore('Streams');
const StreamRulesStore = StoreProvider.getStore('StreamRules');

import StreamRuleForm from 'components/streamrules/StreamRuleForm';
import UserNotification from 'util/UserNotification';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

const Stream = React.createClass({
  propTypes() {
    return {
      stream: PropTypes.object.isRequired,
      permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
      streamRuleTypes: PropTypes.array.isRequired,
      user: PropTypes.object.isRequired,
    };
  },
  mixins: [PermissionsMixin],

  getInitialState() {
    return {
      loading: false,
    };
  },

  _formatNumberOfStreamRules(stream) {
    if (stream.is_default) {
      return 'The default stream contains all messages.';
    }
    let verbalMatchingType;
    switch (stream.matching_type) {
      case 'OR': verbalMatchingType = 'at least one'; break;
      default:
      case 'AND': verbalMatchingType = 'all'; break;
    }
    return (stream.rules.length > 0 ?
    `Must match ${verbalMatchingType} of the ${stream.rules.length} configured stream rule(s).` : 'No configured rules.');
  },
  _onDelete(stream) {
    if (window.confirm('Do you really want to remove this stream?')) {
      StreamsStore.remove(stream.id, () => UserNotification.success(`Stream '${stream.title}' was deleted successfully.`, 'Success'));
    }
  },
  _onResume() {
    this.setState({ loading: true });
    StreamsStore.resume(this.props.stream.id, () => {})
      .finally(() => this.setState({ loading: false }));
  },
  _onUpdate(streamId, stream) {
    StreamsStore.update(streamId, stream, () => UserNotification.success(`Stream '${stream.title}' was updated successfully.`, 'Success'));
  },
  _onClone(streamId, stream) {
    StreamsStore.cloneStream(streamId, stream, () => UserNotification.success(`Stream was successfully cloned as '${stream.title}'.`, 'Success'));
  },
  _onPause() {
    if (window.confirm(`Do you really want to pause stream '${this.props.stream.title}'?`)) {
      this.setState({ loading: true });
      StreamsStore.pause(this.props.stream.id, () => {})
        .finally(() => this.setState({ loading: false }));
    }
  },
  _onQuickAdd() {
    this.refs.quickAddStreamRuleForm.open();
  },
  _onSaveStreamRule(streamRuleId, streamRule) {
    StreamRulesStore.create(this.props.stream.id, streamRule, () => UserNotification.success('Stream rule was created successfully.', 'Success'));
  },
  render() {
    const stream = this.props.stream;
    const permissions = this.props.permissions;

    const isDefaultStream = stream.is_default;
    let editRulesLink;
    let manageOutputsLink;
    let manageAlertsLink;
    if (this.isPermitted(permissions, [`streams:edit:${stream.id}`])) {
      editRulesLink = isDefaultStream ? null : (
        <LinkContainer to={Routes.stream_edit(stream.id)}>
          <Button bsStyle="info">Manage Rules</Button>
        </LinkContainer>
      );
      manageAlertsLink = isDefaultStream ? null : (
        <LinkContainer to={Routes.stream_alerts(stream.id)}>
          <Button bsStyle="info">Manage Alerts</Button>
        </LinkContainer>
      );

      if (this.isPermitted(permissions, ['stream_outputs:read'])) {
        manageOutputsLink = (
          <LinkContainer to={Routes.stream_outputs(stream.id)}>
            <Button bsStyle="info">Manage Outputs</Button>
          </LinkContainer>
        );
      }
    }

    let toggleStreamLink;
    if (this.isAnyPermitted(permissions, [`streams:changestate:${stream.id}`, `streams:edit:${stream.id}`]) && !isDefaultStream) {
      if (stream.disabled) {
        toggleStreamLink = (
          <Button bsStyle="success" className="toggle-stream-button" onClick={this._onResume} disabled={this.state.loading}>
            {this.state.loading ? 'Starting...' : 'Start Stream'}
          </Button>
        );
      } else {
        toggleStreamLink = (
          <Button bsStyle="primary" className="toggle-stream-button" onClick={this._onPause} disabled={this.state.loading}>
            {this.state.loading ? 'Pausing...' : 'Pause Stream'}
          </Button>
        );
      }
    }

    const createdFromContentPack = (stream.content_pack ?
      <i className="fa fa-cube" title="Created from content pack"/> : null);

    const streamRuleList = isDefaultStream ? null :
                           (<CollapsibleStreamRuleList key={`streamRules-${stream.id}`}
                                 stream={stream}
                                 streamRuleTypes={this.props.streamRuleTypes}
                                 permissions={this.props.permissions}/>);
    const streamControls = isDefaultStream ? null :
                           (<StreamControls stream={stream} permissions={this.props.permissions}
                                user={this.props.user}
                                onDelete={this._onDelete} onUpdate={this._onUpdate}
                                onClone={this._onClone}
                                onQuickAdd={this._onQuickAdd}/>);
    return (
      <li className="stream">
        <h2>
          <LinkContainer to={Routes.stream_search(stream.id)}>
            <a>{stream.title}</a>
          </LinkContainer>

          <StreamStateBadge stream={stream} onClick={this._onResume}/>
        </h2>

        <div className="stream-data">
          <div className="stream-actions pull-right">
            {editRulesLink}{' '}
            {manageOutputsLink}{' '}
            {manageAlertsLink}{' '}
            {toggleStreamLink}{' '}

            {streamControls}
          </div>
          <div className="stream-description">
            {createdFromContentPack}

            {stream.description}
          </div>
          <div className="stream-metadata">
            <StreamThroughput streamId={stream.id}/>

            , {this._formatNumberOfStreamRules(stream)}

            {streamRuleList}
          </div>
        </div>
        <StreamRuleForm ref="quickAddStreamRuleForm" title="New Stream Rule"
                        onSubmit={this._onSaveStreamRule}
                        streamRuleTypes={this.props.streamRuleTypes}/>
      </li>
    );
  },
});

export default Stream;
