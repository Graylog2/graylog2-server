import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import { Link } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';
import styled from 'styled-components';

import { Button, Tooltip } from 'components/graylog';
import { OverlayElement, Icon } from 'components/common';
import StreamRuleForm from 'components/streamrules/StreamRuleForm';

import PermissionsMixin from 'util/PermissionsMixin';
import UserNotification from 'util/UserNotification';
import StoreProvider from 'injection/StoreProvider';
import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';

import StreamMetaData from './StreamMetaData';
import StreamControls from './StreamControls';
import StreamStateBadge from './StreamStateBadge';

const StreamsStore = StoreProvider.getStore('Streams');
const StreamRulesStore = StoreProvider.getStore('StreamRules');

const StreamListItem = styled.li(({ theme }) => `
  display: block;
  padding: 15px 0;

  &:not(:last-child) {
    border-bottom: 1px solid ${theme.color.variant.light.info};
  }

  .stream-data {
    margin-top: 8px;

    .stream-actions {
      position: relative;
      float: right;
      right: 0;
      bottom: 20px;

      form.action-form {
        display: inline-block;
      }

      .btn-delete {
        margin-left: 15px;
        margin-right: 15px;

        &.last {
          margin-right: 0;
        }
      }
    }
  }

  .stream-description {
    margin-bottom: 3px;

    .fa-cube {
      margin-right: 5px;
    }
  }
`);

const ToggleButton = styled(Button)`
  width: 8.5em;
`;

const Stream = createReactClass({
  displayName: 'Stream',

  propTypes() {
    return {
      stream: PropTypes.object.isRequired,
      permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
      streamRuleTypes: PropTypes.array.isRequired,
      user: PropTypes.object.isRequired,
      indexSets: PropTypes.array.isRequired,
    };
  },

  mixins: [PermissionsMixin],

  getInitialState() {
    return {
      loading: false,
    };
  },


  _onDelete(stream) {
    // eslint-disable-next-line no-alert
    if (window.confirm('Do you really want to remove this stream?')) {
      StreamsStore.remove(stream.id, (response) => {
        UserNotification.success(`Stream '${stream.title}' was deleted successfully.`, 'Success');
        return response;
      });
    }
  },

  _onResume() {
    const { stream } = this.props;

    this.setState({ loading: true });
    StreamsStore.resume(stream.id, (response) => response)
      .finally(() => this.setState({ loading: false }));
  },

  _onUpdate(streamId, stream) {
    StreamsStore.update(streamId, stream, (response) => {
      UserNotification.success(`Stream '${stream.title}' was updated successfully.`, 'Success');
      return response;
    });
  },

  _onClone(streamId, stream) {
    StreamsStore.cloneStream(streamId, stream, (response) => {
      UserNotification.success(`Stream was successfully cloned as '${stream.title}'.`, 'Success');
      return response;
    });
  },

  _onPause() {
    const { stream } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to pause stream '${stream.title}'?`)) {
      this.setState({ loading: true });
      StreamsStore.pause(stream.id, (response) => response)
        .finally(() => this.setState({ loading: false }));
    }
  },

  _onQuickAdd() {
    this.quickAddStreamRuleForm.open();
  },

  _onSaveStreamRule(streamRuleId, streamRule) {
    const { stream } = this.props;
    StreamRulesStore.create(stream.id, streamRule, () => UserNotification.success('Stream rule was created successfully.', 'Success'));
  },

  render() {
    const { indexSets, stream, permissions, streamRuleTypes, user } = this.props;
    const { loading } = this.state;

    const isDefaultStream = stream.is_default;
    const defaultStreamTooltip = isDefaultStream
      ? <Tooltip id="default-stream-tooltip">Action not available for the default stream</Tooltip> : null;

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

      manageAlertsLink = (
        <LinkContainer to={Routes.stream_alerts(stream.id)}>
          <Button bsStyle="info">Manage Alerts</Button>
        </LinkContainer>
      );

      if (this.isPermitted(permissions, ['stream_outputs:read']) && !AppConfig.isCloud()) {
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
            <ToggleButton bsStyle="success"
                          onClick={this._onResume}
                          disabled={isDefaultStream || loading}>
              {loading ? 'Starting...' : 'Start Stream'}
            </ToggleButton>
          </OverlayElement>
        );
      } else {
        toggleStreamLink = (
          <OverlayElement overlay={defaultStreamTooltip} placement="top" useOverlay={isDefaultStream}>
            <ToggleButton bsStyle="primary"
                          onClick={this._onPause}
                          disabled={isDefaultStream || loading}>
              {loading ? 'Pausing...' : 'Pause Stream'}
            </ToggleButton>
          </OverlayElement>
        );
      }
    }

    const createdFromContentPack = (stream.content_pack
      ? <Icon name="cube" title="Created from content pack" /> : null);

    const streamControls = (
      <OverlayElement overlay={defaultStreamTooltip} placement="top" useOverlay={isDefaultStream}>
        <StreamControls stream={stream}
                        permissions={permissions}
                        user={user}
                        onDelete={this._onDelete}
                        onUpdate={this._onUpdate}
                        onClone={this._onClone}
                        onQuickAdd={this._onQuickAdd}
                        indexSets={indexSets}
                        isDefaultStream={isDefaultStream} />
      </OverlayElement>
    );

    const indexSet = indexSets.find((is) => is.id === stream.index_set_id) || indexSets.find((is) => is.is_default);
    const indexSetDetails = this.isPermitted(permissions, ['indexsets:read']) && indexSet ? <span>index set <em>{indexSet.title}</em> &nbsp;</span> : null;

    return (
      <StreamListItem>
        <div className="stream-actions pull-right">
          {editRulesLink}{' '}
          {manageOutputsLink}{' '}
          {manageAlertsLink}{' '}
          {toggleStreamLink}{' '}

          {streamControls}
        </div>

        <h2>
          <Link to={Routes.stream_search(stream.id)}>{stream.title}</Link>
          {' '}
          <small>{indexSetDetails}<StreamStateBadge stream={stream} /></small>
        </h2>

        <div className="stream-data">
          <div className="stream-description">
            {createdFromContentPack}

            {stream.description}
          </div>
          <StreamMetaData stream={stream}
                          streamRuleTypes={streamRuleTypes}
                          permissions={permissions}
                          isDefaultStream={isDefaultStream} />
        </div>
        <StreamRuleForm ref={(quickAddStreamRuleForm) => { this.quickAddStreamRuleForm = quickAddStreamRuleForm; }}
                        title="New Stream Rule"
                        onSubmit={this._onSaveStreamRule}
                        streamRuleTypes={streamRuleTypes} />
      </StreamListItem>
    );
  },
});

export default Stream;
