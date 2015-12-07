import React, { PropTypes } from 'react';
import StreamThroughput from './StreamThroughput';
import StreamControls from './StreamControls';
import StreamStateBadge from './StreamStateBadge';
import CollapsibleStreamRuleList from 'components/streamrules/CollapsibleStreamRuleList';
import PermissionsMixin from 'util/PermissionsMixin';
import StreamsStore from 'stores/streams/StreamsStore';
import StreamRulesStore from 'stores/streams/StreamRulesStore';
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
      onResume: PropTypes.func.isRequired,
    };
  },
  mixins: [PermissionsMixin],
  render() {
    const stream = this.props.stream;
    const permissions = this.props.permissions;

    let editRulesLink;
    let manageOutputsLink;
    let manageAlertsLink;
    if (this.isPermitted(permissions, ['streams:edit:' + stream.id])) {
      editRulesLink = (
        <LinkContainer to={Routes.stream_edit(stream.id)}>
          <Button bsStyle="info">Edit rules</Button>
        </LinkContainer>
      );
      manageAlertsLink = (
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
    if (this.isAnyPermitted(permissions, ['streams:changestate:' + stream.id, 'streams:edit:' + stream.id])) {
      if (stream.disabled) {
        toggleStreamLink = (
          <a className="btn btn-success toggle-stream-button" onClick={this._onResume}>Start stream</a>
        );
      } else {
        toggleStreamLink = (
          <a className="btn btn-primary toggle-stream-button" onClick={this._onPause}>Pause stream</a>
        );
      }
    }

    const createdFromContentPack = (stream.content_pack ?
      <i className="fa fa-cube" title="Created from content pack"></i> : null);

    return (
      <li className="stream">
        <h2>
          <LinkContainer to={Routes.stream_search(stream.id)}>
            <a>{stream.title}</a>
          </LinkContainer>

          <StreamStateBadge stream={stream} onClick={this.props.onResume}/>
        </h2>

        <div className="stream-data">
          <div className="stream-actions pull-right">
            {editRulesLink}{' '}
            {manageOutputsLink}{' '}
            {manageAlertsLink}{' '}
            {toggleStreamLink}{' '}

            <StreamControls stream={stream} permissions={this.props.permissions} user={this.props.user}
                            onDelete={this._onDelete} onUpdate={this._onUpdate} onClone={this._onClone}
                            onQuickAdd={this._onQuickAdd}/>
          </div>
          <div className="stream-description">
            {createdFromContentPack}

            {stream.description}
          </div>
          <div className="stream-metadata">
            <StreamThroughput streamId={stream.id}/>

            , {this._formatNumberOfStreamRules(stream)}

            <CollapsibleStreamRuleList key={'streamRules-' + stream.id} stream={stream}
                                       streamRuleTypes={this.props.streamRuleTypes}
                                       permissions={this.props.permissions}/>
          </div>
        </div>
        <StreamRuleForm ref="quickAddStreamRuleForm" title="New Stream Rule" onSubmit={this._onSaveStreamRule}
                        streamRuleTypes={this.props.streamRuleTypes}/>
      </li>
    );
  },
  _formatNumberOfStreamRules(stream) {
    let verbalMatchingType;
    switch (stream.matching_type) {
      case 'OR': verbalMatchingType = 'at least one'; break;
      default:
      case 'AND': verbalMatchingType = 'all'; break;
    }
    return (stream.rules.length > 0 ?
      'Must match ' + verbalMatchingType + ' of the ' + stream.rules.length + ' configured stream rule(s).' : 'No configured rules.');
  },
  _onDelete(stream) {
    if (window.confirm('Do you really want to remove this stream?')) {
      StreamsStore.remove(stream.id, () => UserNotification.success('Stream \'' + stream.title + '\' was deleted successfully.', 'Success'));
    }
  },
  _onResume() {
    StreamsStore.resume(this.props.stream.id, () => {
    });
  },
  _onUpdate(streamId, stream) {
    StreamsStore.update(streamId, stream, () => UserNotification.success('Stream \'' + stream.title + '\' was updated successfully.', 'Success'));
  },
  _onClone(streamId, stream) {
    StreamsStore.cloneStream(streamId, stream, () => UserNotification.success('Stream was successfully cloned as \'' + stream.title + '\'.', 'Success'));
  },
  _onPause() {
    if (window.confirm('Do you really want to pause stream \'' + this.props.stream.title + '\'?')) {
      StreamsStore.pause(this.props.stream.id, () => {
      });
    }
  },
  _onQuickAdd() {
    this.refs.quickAddStreamRuleForm.open();
  },
  _onSaveStreamRule(streamRuleId, streamRule) {
    StreamRulesStore.create(this.props.stream.id, streamRule, () => UserNotification.success('Stream rule was created successfully.', 'Success'));
  },
});

export default Stream;
