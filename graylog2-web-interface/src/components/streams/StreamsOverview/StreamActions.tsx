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
import { useTranslation } from 'react-i18next';

import { ShareButton, IfPermitted, HoverForHelp, LinkContainer, IconButton } from 'components/common';
import { ButtonToolbar, MenuItem, DeleteMenuItem } from 'components/bootstrap';
import type { Stream } from 'logic/streams/types';
import useStreamMutations from 'hooks/useStreamMutations';
import Routes from 'routing/Routes';
import { setStartpage } from 'api/startpage';
import StreamRuleModal from 'components/streamrules/StreamRuleModal';
import EntityShareModal from 'components/permissions/EntityShareModal';
import useCurrentUser from 'hooks/useCurrentUser';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import { MoreActions } from 'components/common/EntityDataTable';
import HideOnCloud from 'util/conditional/HideOnCloud';
import UserNotification from 'util/UserNotification';
import StreamDeleteModal from 'components/streams/StreamsOverview/StreamDeleteModal';
import StreamModal from 'components/streams/StreamModal';
import usePluggableEntitySharedActions from 'hooks/usePluggableEntitySharedActions';
import useCreateStreamRule from 'components/streamrules/hooks/useCreateStreamRule';
import StartStreamAfterRuleCreateDialog from 'components/streamrules/StartStreamAfterRuleCreateDialog';

const DefaultStreamHelp = () => {
  const { t } = useTranslation('streams');

  return <HoverForHelp displayLeftMargin>{t('streams_overview.actions.not_available_default')}</HoverForHelp>;
};

const StreamActions = ({ stream, indexSets }: { stream: Stream; indexSets: Array<IndexSet> }) => {
  const { t } = useTranslation('streams');
  const currentUser = useCurrentUser();
  const { pauseStream, resumeStream, updateStream, removeStream, cloneStream } = useStreamMutations();
  const { deselectEntity } = useSelectedEntities();
  const [showDeleteModal, setDeleteModal] = useState(false);
  const [showEntityShareModal, setShowEntityShareModal] = useState(false);
  const [showStreamRuleModal, setShowStreamRuleModal] = useState(false);
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [showCloneModal, setShowCloneModal] = useState(false);
  const [changingStatus, setChangingStatus] = useState(false);
  const sendTelemetry = useSendTelemetry();
  const handleSetStartpage = useCallback(
    () => setStartpage(currentUser.id, 'stream', stream.id),
    [currentUser.id, stream.id],
  );
  const { actions: pluggableActions, actionModals: pluggableActionModals } = usePluggableEntitySharedActions<Stream>(
    stream,
    'stream',
  );
  const moreActions = [pluggableActions.length ? pluggableActions : null].filter(Boolean);

  const isDefaultStream = stream.is_default;
  const isNotEditable = !stream.is_editable;
  const onToggleStreamStatus = useCallback(async () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_STATUS_TOGGLED, {
      app_pathname: 'streams',
      app_action_value: ' stream-item-status',
      event_details: {
        enabled: !stream.disabled,
      },
    });

    setChangingStatus(true);

    // The api fns handle the error toast on rejection, so we swallow it here to avoid an unhandled rejection.
    if (stream.disabled) {
      await resumeStream(stream.id).catch(() => {});
    }

    // eslint-disable-next-line no-alert
    if (!stream.disabled && window.confirm(t('streams_overview.actions.pause_confirm', { title: stream.title }))) {
      await pauseStream(stream.id).catch(() => {});
    }

    setChangingStatus(false);
  }, [sendTelemetry, stream.disabled, stream.id, stream.title, resumeStream, pauseStream, t]);

  const toggleEntityShareModal = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_SHARE_MODAL_OPENED, {
      app_pathname: 'streams',
    });

    setShowEntityShareModal((cur) => !cur);
  }, [sendTelemetry]);

  const toggleDeleteModal = useCallback(() => {
    setDeleteModal((cur) => !cur);
  }, []);

  const toggleUpdateModal = useCallback(() => {
    setShowUpdateModal((cur) => !cur);
  }, []);

  const toggleCloneModal = useCallback(() => {
    setShowCloneModal((cur) => !cur);
  }, []);

  const toggleStreamRuleModal = useCallback(() => {
    setShowStreamRuleModal((cur) => !cur);
  }, []);

  const onDelete = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_DELETED, {
      app_pathname: 'streams',
      app_action_value: 'stream-item-delete',
    });

    removeStream(stream.id)
      .then(() => {
        deselectEntity(stream.id);
        UserNotification.success(
          t('streams_overview.actions.deleted', { title: stream.title }),
          t('streams_overview.actions.success_title'),
        );
        toggleDeleteModal();
      })
      .catch((error) => {
        UserNotification.error(t('streams_overview.actions.delete_failed', { error }));
      });
  }, [deselectEntity, removeStream, sendTelemetry, stream.id, stream.title, toggleDeleteModal, t]);

  const { onCreateStreamRule, showStartStreamDialog, onCancelStartStreamDialog, onStartStream, isStartingStream } =
    useCreateStreamRule({
      streamId: stream.id,
      streamIsPaused: stream.disabled,
    });

  const onUpdate = useCallback(
    (newStream: Stream) =>
      updateStream({ streamId: stream.id, data: newStream }).then(() => {
        sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_UPDATED, {
          app_pathname: 'streams',
        });

        UserNotification.success(
          t('streams_overview.actions.updated', { title: newStream.title }),
          t('streams_overview.actions.success_title'),
        );
      }),
    [sendTelemetry, stream.id, updateStream, t],
  );

  const onCloneSubmit = useCallback(
    (newStream: Stream) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_CLONED, {
        app_pathname: 'streams',
      });

      return cloneStream({
        streamId: stream.id,
        data: {
          title: newStream.title,
          description: newStream.description,
          index_set_id: newStream.index_set_id,
          remove_matches_from_default_stream: newStream.remove_matches_from_default_stream,
        },
      }).then(() => {
        UserNotification.success(
          t('streams_overview.actions.cloned', { title: newStream.title }),
          t('streams_overview.actions.success_title'),
        );
      });
    },
    [sendTelemetry, stream.id, cloneStream, t],
  );

  return (
    <ButtonToolbar>
      <IfPermitted permissions={`streams:read:${stream.id}`}>
        <LinkContainer to={Routes.stream_view(stream.id)}>
          <IconButton
            name="call_split"
            title={t('streams_overview.actions.data_routing')}
            bsStyle="default"
            disabled={isNotEditable}
            onClick={() => {
              sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_DATA_ROUTING_CLICKED, {
                app_pathname: 'stream',
              });
            }}
          />
        </LinkContainer>
      </IfPermitted>
      <ShareButton entityId={stream.id} entityType="stream" onClick={toggleEntityShareModal} bsSize="xs" />
      <MoreActions disabled={isNotEditable}>
        <IfPermitted permissions={[`streams:changestate:${stream.id}`, `streams:edit:${stream.id}`]} anyPermissions>
          <MenuItem onSelect={onToggleStreamStatus} disabled={isDefaultStream || isNotEditable}>
            {changingStatus ? (
              <span>
                {stream.disabled ? t('streams_overview.actions.starting') : t('streams_overview.actions.stopping')}
              </span>
            ) : (
              <span>{stream.disabled ? t('streams_overview.actions.start') : t('streams_overview.actions.stop')}</span>
            )}
            {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem onSelect={toggleStreamRuleModal} disabled={isDefaultStream}>
            {t('streams_overview.actions.quick_add_rule')} {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem onSelect={toggleUpdateModal} disabled={isDefaultStream}>
            {t('streams_overview.actions.edit_stream')} {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>

        <IfPermitted permissions={[`streams:edit:${stream.id}`]}>
          <MenuItem divider />
        </IfPermitted>

        <IfPermitted permissions={[`streams:edit:${stream.id}`]}>
          <MenuItem disabled={isDefaultStream || isNotEditable} href={Routes.stream_edit(stream.id)}>
            {t('streams_overview.actions.manage_rules')} {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>
        <HideOnCloud>
          <IfPermitted permissions="stream_outputs:read">
            <MenuItem href={Routes.stream_outputs(stream.id)}>{t('streams_overview.actions.manage_outputs')}</MenuItem>
          </IfPermitted>
        </HideOnCloud>
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem href={Routes.stream_alerts(stream.id)}>{t('streams_overview.actions.manage_alerts')}</MenuItem>
        </IfPermitted>
        {moreActions}
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem divider />
        </IfPermitted>

        <MenuItem onSelect={handleSetStartpage} disabled={currentUser.readOnly}>
          {t('streams_overview.actions.set_start_page')}
        </MenuItem>

        <IfPermitted permissions={['streams:create', `streams:read:${stream.id}`]}>
          <MenuItem onSelect={toggleCloneModal} disabled={isDefaultStream}>
            {t('streams_overview.actions.clone_stream')} {isDefaultStream && <DefaultStreamHelp />}
          </MenuItem>
        </IfPermitted>

        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <DeleteMenuItem onSelect={toggleDeleteModal} disabled={isDefaultStream}>
            {t('streams_overview.actions.delete_stream')} {isDefaultStream && <DefaultStreamHelp />}
          </DeleteMenuItem>
        </IfPermitted>
      </MoreActions>
      {showUpdateModal && (
        <StreamModal
          title={t('streams_overview.actions.edit_title')}
          onSubmit={onUpdate}
          onClose={toggleUpdateModal}
          submitButtonText={t('streams_overview.actions.update_button')}
          submitLoadingText={t('streams_overview.actions.updating')}
          initialValues={stream}
          indexSets={indexSets}
        />
      )}
      {showCloneModal && (
        <StreamModal
          title={t('streams_overview.actions.clone_title')}
          initialValues={stream}
          onSubmit={onCloneSubmit}
          onClose={toggleCloneModal}
          submitButtonText={t('streams_overview.actions.clone_button')}
          submitLoadingText={t('streams_overview.actions.cloning')}
          indexSets={indexSets}
        />
      )}
      {showStreamRuleModal && (
        <StreamRuleModal
          onClose={toggleStreamRuleModal}
          title={t('stream_rules.new_title')}
          submitButtonText={t('stream_rules.create_button')}
          submitLoadingText={t('stream_rules.creating')}
          onSubmit={onCreateStreamRule}
        />
      )}
      {showEntityShareModal && (
        <EntityShareModal
          entityId={stream.id}
          entityType="stream"
          entityTitle={stream.title}
          description={t('stream_modal.share_description')}
          onClose={toggleEntityShareModal}
        />
      )}
      <StartStreamAfterRuleCreateDialog
        show={showStartStreamDialog}
        streamTitle={stream.title}
        onConfirm={onStartStream}
        onCancel={onCancelStartStreamDialog}
        isSubmitting={isStartingStream}
      />
      {showDeleteModal && (
        <StreamDeleteModal
          streamTitle={stream.title}
          streamId={stream.id}
          onCancel={toggleDeleteModal}
          onDelete={onDelete}
        />
      )}
      {pluggableActionModals}
    </ButtonToolbar>
  );
};

export default StreamActions;
