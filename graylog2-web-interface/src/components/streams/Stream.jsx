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
import styled, { css } from 'styled-components';

import EntityShareModal from 'components/permissions/EntityShareModal';
import { Link } from 'components/common/router';
import { Tooltip, ButtonToolbar } from 'components/bootstrap';
import { Icon, OverlayElement } from 'components/common';
import StreamRuleModal from 'components/streamrules/StreamRuleModal';
import { isPermitted } from 'util/PermissionsMixin';
import UserNotification from 'util/UserNotification';
import Routes from 'routing/Routes';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import ObjectUtils from 'util/ObjectUtils';

import StreamMetaData from './StreamMetaData';
import StreamActions from './StreamActions';
import StreamStateBadge from './StreamStateBadge';

const StreamListItem = styled.li(({ theme }) => css`
  display: block;
  padding: 15px 0;

  &:not(:last-child) {
    border-bottom: 1px solid ${theme.colors.gray[90]};
  }

  .stream-data {
    margin-top: 8px;
  }

  .stream-description {
    margin-bottom: 3px;

    .fa-cube {
      margin-right: 5px;
    }
  }
  
  .overlay-trigger {
    float: left;
    margin-left: 5px;
  }
`);

const StreamTitle = styled.h2(({ theme }) => `
  font-family: ${theme.fonts.family.body};
`);

const _onDelete = (stream) => {
  // eslint-disable-next-line no-alert
  if (window.confirm('Do you really want to remove this stream?')) {
    StreamsStore.remove(stream.id, (response) => {
      UserNotification.success(`Stream '${stream.title}' was deleted successfully.`, 'Success');

      return response;
    });
  }
};

const _onUpdate = (streamId, _stream) => {
  const stream = ObjectUtils.trimObjectFields(_stream, ['title']);

  StreamsStore.update(streamId, stream, (response) => {
    UserNotification.success(`Stream '${stream.title}' was updated successfully.`, 'Success');

    return response;
  });
};

const _onClone = (streamId, _stream) => {
  const stream = ObjectUtils.trimObjectFields(_stream, ['title']);

  StreamsStore.cloneStream(streamId, stream, (response) => {
    UserNotification.success(`Stream was successfully cloned as '${stream.title}'.`, 'Success');

    return response;
  });
};

class Stream extends React.Component {
  static propTypes = {
    stream: PropTypes.object.isRequired,
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
    streamRuleTypes: PropTypes.array.isRequired,
    user: PropTypes.object.isRequired,
    indexSets: PropTypes.array.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      showStreamRuleForm: false,
      showEntityShareModal: false,
    };
  }

  _closeStreamRuleForm = () => {
    this.setState({ showStreamRuleForm: false });
  };

  _openStreamRuleForm = () => {
    this.setState({ showStreamRuleForm: true });
  };

  _closeEntityShareModal = () => {
    this.setState({ showEntityShareModal: false });
  };

  _onSaveStreamRule = (streamRuleId, streamRule) => {
    const { stream } = this.props;

    StreamRulesStore.create(stream.id, streamRule, () => UserNotification.success('Stream rule was created successfully.', 'Success'));
  };

  render() {
    const { indexSets, stream, permissions, streamRuleTypes, user } = this.props;
    const { showStreamRuleForm, showEntityShareModal } = this.state;

    const isDefaultStream = stream.is_default;
    const isNotEditable = !stream.is_editable;
    const defaultStreamTooltip = isDefaultStream
      ? <Tooltip id="default-stream-tooltip">Action not available for the default stream</Tooltip> : null;

    const createdFromContentPack = (stream.content_pack
      ? <Icon name="cube" title="Created from content pack" /> : null);

    const indexSet = indexSets.find((is) => is.id === stream.index_set_id) || indexSets.find((is) => is.is_default);
    const indexSetDetails = isPermitted(permissions, ['indexsets:read']) && indexSet ? <span>index set <em>{indexSet.title}</em> &nbsp;</span> : null;

    return (
      <StreamListItem>
        <ButtonToolbar className="pull-right">
          <OverlayElement overlay={defaultStreamTooltip} placement="top" className="overlay-trigger">
            <StreamActions stream={stream}
                           permissions={permissions}
                           user={user}
                           streamRuleTypes={streamRuleTypes}
                           onDelete={_onDelete}
                           onUpdate={_onUpdate}
                           onClone={_onClone}
                           onQuickAdd={this._openStreamRuleForm}
                           indexSets={indexSets}
                           isDefaultStream={isDefaultStream}
                           disabled={isNotEditable} />
          </OverlayElement>
        </ButtonToolbar>

        <StreamTitle>
          <Link to={Routes.stream_search(stream.id)}>{stream.title}</Link>
          {' '}
          <small>{indexSetDetails}<StreamStateBadge stream={stream} /></small>
        </StreamTitle>

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
        {showStreamRuleForm && (
          <StreamRuleModal onClose={this._closeStreamRuleForm}
                           title="New Stream Rule"
                           submitButtonText="Create rule"
                           submitLoadingText="Creating rule..."
                           onSubmit={this._onSaveStreamRule}
                           streamRuleTypes={streamRuleTypes} />
        )}
        {showEntityShareModal && (
          <EntityShareModal entityId={stream.id}
                            entityType="stream"
                            entityTitle={stream.title}
                            description="Search for a User or Team to add as collaborator on this stream."
                            onClose={this._closeEntityShareModal} />
        )}
      </StreamListItem>
    );
  }
}

export default Stream;
