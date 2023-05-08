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
import * as React from 'react';
import { useState, useCallback } from 'react';

import { ShareButton, IfPermitted, HoverForHelp } from 'components/common';
import { ButtonToolbar, MenuItem } from 'components/bootstrap';
import type { Stream, StreamRule } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { StartpageStore } from 'stores/users/StartpageStore';
import UserNotification from 'util/UserNotification';
import StreamRuleModal from 'components/streamrules/StreamRuleModal';
import EntityShareModal from 'components/permissions/EntityShareModal';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import useCurrentUser from 'hooks/useCurrentUser';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import StreamModal from '../StreamModal';

const DefaultStreamHelp = () => (
  <HoverForHelp displayLeftMargin>Action not available for the default
    stream
  </HoverForHelp>
);

const StreamActions = ({
  stream,
  indexSets,
}: {
  stream: Stream,
  indexSets: Array<IndexSet>,
}) => {
  const currentUser = useCurrentUser();
  const [showEntityShareModal, setShowEntityShareModal] = useState(false);
  const [showStreamRuleModal, setShowStreamRuleModal] = useState(false);
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [showCloneModal, setShowCloneModal] = useState(false);
  const [changingStatus, setChangingStatus] = useState(false);
  const sendTelemetry = useSendTelemetry();
  const setStartpage = useCallback(() => StartpageStore.set(currentUser.id, 'stream', stream.id), [stream.id, currentUser.id]);

  const isDefaultStream = stream.is_default;
  const isNotEditable = !stream.is_editable;
  const onToggleStreamStatus = useCallback(async () => {
    sendTelemetry('input_button_toggle', {
      app_pathname: 'streams',
      app_action_value: ' stream-item-status',
    });

    setChangingStatus(true);

    if (stream.disabled) {
      await StreamsStore.resume(stream.id, (response) => response);
    }

    // eslint-disable-next-line no-alert
    if (!stream.disabled && window.confirm(`Do you really want to pause stream '${stream.title}'?`)) {
      await StreamsStore.pause(stream.id, (response) => response);
    }

    setChangingStatus(false);
  }, [sendTelemetry, stream.disabled, stream.id, stream.title]);

  const toggleEntityShareModal = useCallback(() => {
    sendTelemetry('click', {
      app_pathname: 'streams',
      app_action_value: 'stream-item-share',
    });

    setShowEntityShareModal((cur) => !cur);
  }, [sendTelemetry]);

  const toggleUpdateModal = useCallback(() => {
    sendTelemetry('click', {
      app_pathname: 'streams',
      app_action_value: 'stream-item-update',
    });

    setShowUpdateModal((cur) => !cur);
  }, [sendTelemetry]);

  const toggleCloneModal = useCallback(() => {
    sendTelemetry('click', {
      app_pathname: 'streams',
      app_action_value: 'stream-item-clone',
    });

    setShowCloneModal((cur) => !cur);
  }, [sendTelemetry]);

  const toggleStreamRuleModal = useCallback(() => {
    sendTelemetry('click', {
      app_pathname: 'streams',
      app_action_value: 'stream-item-rule',
    });

    setShowStreamRuleModal((cur) => !cur);
  }, [sendTelemetry]);

  const onDelete = useCallback(() => {
    sendTelemetry('click', {
      app_pathname: 'streams',
      app_action_value: 'stream-item-delete',
    });

    // eslint-disable-next-line no-alert
    if (window.confirm('Do you really want to remove this stream?')) {
      StreamsStore.remove(stream.id, (response) => {
        UserNotification.success(`Stream '${stream.title}' was deleted successfully.`, 'Success');

        return response;
      }).catch((error) => {
        UserNotification.error(`An error occurred while deleting the stream. ${error}`);
      });
    }
  }, [sendTelemetry, stream.id, stream.title]);

  const onSaveStreamRule = useCallback((_streamRuleId: string, streamRule: StreamRule) => {
    return StreamRulesStore.create(stream.id, streamRule, () => UserNotification.success('Stream rule was created successfully.', 'Success'));
  }, [stream.id]);

  const onUpdate = useCallback((newStream: Stream) => {
    return StreamsStore.update(stream.id, newStream, (response) => {
      UserNotification.success(`Stream '${newStream.title}' was updated successfully.`, 'Success');

      return response;
    });
  }, [stream.id]);

  const onCloneSubmit = useCallback((newStream: Stream) => {
    return StreamsStore.cloneStream(stream.id, newStream, (response) => {
      UserNotification.success(`Stream was successfully cloned as '${newStream.title}'.`, 'Success');

      return response;
    });
  }, [stream.id]);

  return (
    <ButtonToolbar>
      <ShareButton entityId={stream.id}
                   entityType="stream"
                   onClick={toggleEntityShareModal}
                   bsSize="xsmall" />
      <OverlayDropdownButton title="More"
                             bsSize="xsmall"
                             buttonTitle="More actions"
                             disabled={isNotEditable}
                             dropdownZIndex={1000}>
        <IfPermitted permissions={[`streams:changestate:${stream.id}`, `streams:edit:${stream.id}`]} anyPermissions>
          <MenuItem bsStyle="success"
                    onSelect={onToggleStreamStatus}
                    disabled={isDefaultStream || isNotEditable}>
            {changingStatus
              ? <span>{stream.disabled ? 'Starting Stream...' : 'Stopping Stream...'}</span>
              : <span>{stream.disabled ? 'Start Stream' : 'Stop Stream'}</span>}
            {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem onSelect={toggleStreamRuleModal} disabled={isDefaultStream}>
            Quick add rule {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem onSelect={toggleUpdateModal} disabled={isDefaultStream}>
            Edit stream {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>

        <IfPermitted permissions={[`streams:edit:${stream.id}`]}>
          <MenuItem divider />
        </IfPermitted>

        <IfPermitted permissions={[`streams:edit:${stream.id}`]}>
          <LinkContainer to={Routes.stream_edit(stream.id)}>
            <MenuItem disabled={isDefaultStream || isNotEditable}>
              Manage Rules {isDefaultStream && <DefaultStreamHelp />}
            </MenuItem>
          </LinkContainer>
        </IfPermitted>
        <HideOnCloud>
          <IfPermitted permissions="stream_outputs:read">
            <LinkContainer to={Routes.stream_outputs(stream.id)}>
              <MenuItem>
                Manage Outputs
              </MenuItem>
            </LinkContainer>
          </IfPermitted>
        </HideOnCloud>
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <LinkContainer to={Routes.stream_alerts(stream.id)}>
            <MenuItem>
              Manage Alerts
            </MenuItem>
          </LinkContainer>
        </IfPermitted>

        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem divider />
        </IfPermitted>

        <MenuItem onSelect={setStartpage} disabled={currentUser.readOnly}>
          Set as startpage
        </MenuItem>

        <IfPermitted permissions={['streams:create', `streams:read:${stream.id}`]}>
          <MenuItem onSelect={toggleCloneModal} disabled={isDefaultStream}>
            Clone this stream {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>

        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem onSelect={onDelete} disabled={isDefaultStream}>
            Delete this stream {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>
      </OverlayDropdownButton>
      {showUpdateModal && (
        <StreamModal title="Editing Stream"
                     onSubmit={onUpdate}
                     onClose={toggleUpdateModal}
                     submitButtonText="Update stream"
                     submitLoadingText="Updating stream..."
                     initialValues={stream}
                     indexSets={indexSets} />
      )}
      {showCloneModal && (
        <StreamModal title="Cloning Stream"
                     onSubmit={onCloneSubmit}
                     onClose={toggleCloneModal}
                     submitButtonText="Clone stream"
                     submitLoadingText="Cloning stream..."
                     indexSets={indexSets} />
      )}
      {showStreamRuleModal && (
        <StreamRuleModal onClose={toggleStreamRuleModal}
                         title="New Stream Rule"
                         submitButtonText="Create Rule"
                         submitLoadingText="Creating Rule..."
                         onSubmit={onSaveStreamRule} />
      )}
      {showEntityShareModal && (
        <EntityShareModal entityId={stream.id}
                          entityType="stream"
                          entityTitle={stream.title}
                          description="Search for a User or Team to add as collaborator on this stream."
                          onClose={toggleEntityShareModal} />
      )}
    </ButtonToolbar>
  );
};

export default StreamActions;
