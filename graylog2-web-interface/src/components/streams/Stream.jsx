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
import { OverlayElement, Pluralize } from 'components/common';
import UserNotification from 'util/UserNotification';
import { Button, Tooltip } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

const Stream = React.createClass({
  propTypes() {
    return {
      stream: PropTypes.object.isRequired,
      permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
      streamRuleTypes: PropTypes.array.isRequired,
      user: PropTypes.object.isRequired,
      indexSets: React.PropTypes.array.isRequired,
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
    if (stream.rules.length === 0) {
      return 'No configured rules.';
    }

    let verbalMatchingType;
    switch (stream.matching_type) {
      case 'OR': verbalMatchingType = 'at least one'; break;
      default:
      case 'AND': verbalMatchingType = 'all'; break;
    }

    return (
      <span>
        Must match {verbalMatchingType} of the {stream.rules.length} configured stream{' '}
        <Pluralize value={stream.rules.length} plural="rules" singular="rule" />.
      </span>
    );
  },
  _onDelete(stream) {
    if (window.confirm('Do you really want to remove this stream?')) {
      StreamsStore.remove(stream.id, response => {
        UserNotification.success(`Stream '${stream.title}' was deleted successfully.`, 'Success');
        return response;
      });
    }
  },
  _onResume() {
    this.setState({ loading: true });
    StreamsStore.resume(this.props.stream.id, response => response)
      .finally(() => this.setState({ loading: false }));
  },
  _onUpdate(streamId, stream) {
    StreamsStore.update(streamId, stream, response => {
      UserNotification.success(`Stream '${stream.title}' was updated successfully.`, 'Success');
      return response;
    });
  },
  _onClone(streamId, stream) {
    StreamsStore.cloneStream(streamId, stream, response => {
      UserNotification.success(`Stream was successfully cloned as '${stream.title}'.`, 'Success');
      return response;
    });
  },
  _onPause() {
    if (window.confirm(`Do you really want to pause stream '${this.props.stream.title}'?`)) {
      this.setState({ loading: true });
      StreamsStore.pause(this.props.stream.id, response => response)
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
    const defaultStreamTooltip = isDefaultStream ?
      <Tooltip id="default-stream-tooltip">Action not available for the default stream</Tooltip> : null;

    let editRulesLink;
    let manageOutputsLink;
    let manageAlertsLink;
    if (this.isPermitted(permissions, [`streams:edit:${stream.id}`])) {
      editRulesLink = (
        <OverlayElement overlay={defaultStreamTooltip} placement="top" useOverlay={isDefaultStream}>
          <LinkContainer disabled={isDefaultStream} to={Routes.stream_edit(stream.id)}>
            <Button bsStyle="info">Manage Rules</Button>
          </LinkContainer>
        </OverlayElement>
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
    if (this.isAnyPermitted(permissions, [`streams:changestate:${stream.id}`, `streams:edit:${stream.id}`])) {
      if (stream.disabled) {
        toggleStreamLink = (
          <OverlayElement overlay={defaultStreamTooltip} placement="top" useOverlay={isDefaultStream}>
            <Button bsStyle="success" className="toggle-stream-button" onClick={this._onResume}
                    disabled={isDefaultStream || this.state.loading}>
              {this.state.loading ? 'Starting...' : 'Start Stream'}
            </Button>
          </OverlayElement>
        );
      } else {
        toggleStreamLink = (
          <OverlayElement overlay={defaultStreamTooltip} placement="top" useOverlay={isDefaultStream}>
            <Button bsStyle="primary" className="toggle-stream-button" onClick={this._onPause}
                    disabled={isDefaultStream || this.state.loading}>
              {this.state.loading ? 'Pausing...' : 'Pause Stream'}
            </Button>
          </OverlayElement>
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
    const streamControls = (
      <OverlayElement overlay={defaultStreamTooltip} placement="top" useOverlay={isDefaultStream}>
        <StreamControls stream={stream} permissions={this.props.permissions}
                        user={this.props.user}
                        onDelete={this._onDelete} onUpdate={this._onUpdate}
                        onClone={this._onClone}
                        onQuickAdd={this._onQuickAdd}
                        indexSets={this.props.indexSets}
                        isDefaultStream={isDefaultStream} />
      </OverlayElement>
    );
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
            <StreamThroughput streamId={stream.id}/>. {this._formatNumberOfStreamRules(stream)}
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
