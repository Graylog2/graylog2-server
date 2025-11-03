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

import MessageFieldsEditModeList from 'views/components/messagelist/MessageFields/MessageFieldsEditModeList';
import { Alert, Modal, Button, ButtonGroup } from 'components/bootstrap';
import StreamLink from 'components/streams/StreamLink';
import { useStore } from 'stores/connect';
import { StreamsStore } from 'views/stores/StreamsStore';
import useFormattedFields from 'views/components/messagelist/MessageFields/hooks/useFormattedFields';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import StringUtils from 'util/StringUtils';
import useMessageFavoriteFieldsForEditing from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsForEditing';

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

const ButtonContainer = styled.div`
  display: flex;
  justify-content: space-between;
  width: 100%;
  align-items: end;
`;

const StyledButtonGroup = styled(ButtonGroup)`
  gap: ${({ theme }) => theme.spacings.xs};
`;

const StyledAlert = styled(Alert)`
  margin-top: 0;
`;

const MessageFieldsEditModal = ({ toggleEditMode }) => {
  const { editingFavoriteFields, saveEditedFavoriteFields, resetFavoriteField, setFavorites } =
    useMessageFavoriteFieldsForEditing();
  const { formattedFavorites, formattedRest } = useFormattedFields(editingFavoriteFields);
  const { message } = useContext(MessageFavoriteFieldsContext);
  const messageStreams = useStore(StreamsStore, ({ streams }) =>
    streams.filter((stream) => message.fields.streams.includes(stream.id)),
  );

  const _saveFavoriteField = useCallback(() => {
    saveEditedFavoriteFields();
    toggleEditMode();
  }, [saveEditedFavoriteFields, toggleEditMode]);

  const _resetFavoriteField = useCallback(() => {
    resetFavoriteField();
    toggleEditMode();
  }, [resetFavoriteField, toggleEditMode]);

  return (
    <Modal onHide={toggleEditMode} show rootProps={{ zIndex: 1030 }}>
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
            <MessageFieldsEditModeList
              setFavorites={setFavorites}
              fields={formattedFavorites}
              message={message}
              isFavorite
            />
            <MessageFieldsEditModeList
              setFavorites={setFavorites}
              fields={formattedRest}
              message={message}
              isFavorite={false}
            />
          </FieldsContainer>
        </ModalContentContainer>
      </Modal.Body>
      <Modal.Footer>
        <ButtonContainer>
          <Button bsStyle="link" onClick={_resetFavoriteField}>
            Reset to default
          </Button>
          <StyledButtonGroup>
            <Button onClick={toggleEditMode}>Cancel</Button>
            <Button bsStyle="primary" onClick={_saveFavoriteField}>
              Save Configuration
            </Button>
          </StyledButtonGroup>
        </ButtonContainer>
      </Modal.Footer>
    </Modal>
  );
};

export default MessageFieldsEditModal;
