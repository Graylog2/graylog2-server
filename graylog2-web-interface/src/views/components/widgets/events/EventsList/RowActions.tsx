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
import { useState, useRef } from 'react';

import { IconButton, ModalSubmit } from 'components/common';
import { ButtonToolbar, Modal, Menu, MenuItem } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';
import Routes from 'routing/Routes';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';

import EventDetails from './EventDetails';

const usePluggableDashboardActions = (eventId: string) => {
  const modalRefs = useRef({});
  const pluggableActions = usePluginEntities('views.components.widgets.events.actions');
  const availableActions = pluggableActions.filter(
    (perspective) => (perspective.useCondition ? !!perspective.useCondition() : true),
  );
  const actions = availableActions.map(({ component: PluggableDashboardAction, key }) => (
    <PluggableDashboardAction key={`event-action-${key}`}
                              eventId={eventId}
                              modalRef={() => modalRefs.current[key]} />
  ));

  const actionModals = availableActions
    .filter(({ modal }) => !!modal)
    .map(({ modal: ActionModal, key }) => (
      <ActionModal key={`event-action-modal-${key}`}
                   eventId={eventId}
                   ref={(r) => { modalRefs.current[key] = r; }} />
    ));

  return ({ actions, actionModals });
};

type Props = {
  eventId: string,
  hasReplayInfo: boolean,
  eventDefinitionId: string,
}

const RowActions = ({ eventId, hasReplayInfo, eventDefinitionId }: Props) => {
  const user = useCurrentUser();
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const toggleDetailsModal = () => setShowDetailsModal((cur) => !cur);
  const { actions: pluggableActions, actionModals: pluggableActionModals } = usePluggableDashboardActions(eventId);

  const moreActions = [
    (hasReplayInfo && isPermitted(user.permissions, `eventdefinitions:read:${eventDefinitionId}`)) ? (
      <MenuItem href={Routes.ALERTS.replay_search(eventId)} target="_blank" key="replay-search">
        Replay search
      </MenuItem>
    ) : null,
    pluggableActions.length ? pluggableActions : null,
  ].filter(Boolean);

  return (
    <>
      <ButtonToolbar>
        <IconButton name="open_in_full" title="View event details" onClick={toggleDetailsModal} />
        {!!moreActions.length && (
          <Menu position="bottom-end">
            <Menu.Target>
              <IconButton name="more_vert" title="Toggle event actions" />
            </Menu.Target>
            <Menu.Dropdown>
              {moreActions}
            </Menu.Dropdown>
          </Menu>
        )}
      </ButtonToolbar>
      {showDetailsModal && (
        <Modal show={showDetailsModal}
               bsSize="large"
               onHide={toggleDetailsModal}>
          <Modal.Header closeButton>
            <Modal.Title>Event details</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <EventDetails eventId={eventId} />
          </Modal.Body>
          <Modal.Footer>
            <ModalSubmit displayCancel={false}
                         onSubmit={toggleDetailsModal}
                         submitButtonType="button"
                         submitButtonText="Close" />
          </Modal.Footer>
        </Modal>
      )}
      {pluggableActionModals}
    </>
  );
};

export default RowActions;
