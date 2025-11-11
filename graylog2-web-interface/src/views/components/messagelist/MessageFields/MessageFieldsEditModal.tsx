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

import React, { useContext, useCallback } from 'react';
import styled, { css } from 'styled-components';

import { Alert, Modal, Button } from 'components/bootstrap';
import StreamLink from 'components/streams/StreamLink';
import { useStore } from 'stores/connect';
import { StreamsStore } from 'views/stores/StreamsStore';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import StringUtils from 'util/StringUtils';
import useMessageFavoriteFieldsForEditing from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsForEditing';
import { ModalSubmit } from 'components/common';
import MessageFieldsEditModeLists from 'views/components/messagelist/MessageFields/MessageFieldsEditModeLists';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const FieldsContainer = styled.div(
  ({ theme }) => css`
    overflow: visible;
    padding: ${theme.spacings.xs} 0;
  `,
);

const ModalContentContainer = styled.div`
  max-height: 500px;
  overflow-y: auto;
  font-size: ${({ theme }) => theme.fonts.size.small};
`;

const StyledAlert = styled(Alert)`
  margin-top: 0;
`;

const MessageFieldsEditModal = ({ toggleEditMode }) => {
  const {
    editingFavoriteFields,
    saveEditedFavoriteFields,
    resetFavoriteFields,
    reorderFavoriteFields,
    onFavoriteToggle,
  } = useMessageFavoriteFieldsForEditing();
  const sendTelemetry = useSendTelemetry();
  const { message } = useContext(MessageFavoriteFieldsContext);
  const messageStreams = useStore(StreamsStore, ({ streams }) =>
    streams.filter((stream) => message.fields.streams.includes(stream.id)),
  );

  const _saveFavoriteField = useCallback(() => {
    saveEditedFavoriteFields();
    toggleEditMode();
  }, [saveEditedFavoriteFields, toggleEditMode]);

  const _resetFavoriteFields = useCallback(() => {
    resetFavoriteFields();
    toggleEditMode();
  }, [resetFavoriteFields, toggleEditMode]);

  const _toggleEditMode = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.CHANGE_MESSAGE_FAVORITE_FIELDS_EDIT_CANCELED, {
      app_action_value: 'canceled',
    });

    return toggleEditMode();
  }, [sendTelemetry, toggleEditMode]);

  return (
    <Modal onHide={_toggleEditMode} show rootProps={{ zIndex: 1030 }}>
      <Modal.Header>
        <Modal.Title>Favorite fields configuration</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <ModalContentContainer>
          <FieldsContainer>
            <StyledAlert bsStyle="info">
              Favorite fields will be applied to the{' '}
              {StringUtils.pluralize(messageStreams.length, ' stream ', ' streams ')}
              {messageStreams.map((stream, index) => {
                const isLast = index === messageStreams.length - 1;

                return (
                  <React.Fragment key={stream.id}>
                    <StreamLink key={stream.id} stream={stream} />
                    {!isLast && ', '}
                  </React.Fragment>
                );
              })}{' '}
              in which this message is routed.
            </StyledAlert>
            <MessageFieldsEditModeLists
              reorderFavoriteFields={reorderFavoriteFields}
              onFavoriteToggle={onFavoriteToggle}
              editingFavoriteFields={editingFavoriteFields}
            />
          </FieldsContainer>
        </ModalContentContainer>
      </Modal.Body>
      <Modal.Footer>
        <ModalSubmit
          submitButtonText="Save Configuration"
          leftCol={
            <Button bsStyle="link" onClick={_resetFavoriteFields}>
              Reset to default
            </Button>
          }
          onCancel={toggleEditMode}
          onSubmit={_saveFavoriteField}
          submitButtonType="button"
        />
      </Modal.Footer>
    </Modal>
  );
};

export default MessageFieldsEditModal;
