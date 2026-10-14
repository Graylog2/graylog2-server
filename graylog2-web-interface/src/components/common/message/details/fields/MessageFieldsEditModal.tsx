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
import { Trans, useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import { Alert, Modal, Button } from 'components/bootstrap';
import StreamLink from 'components/streams/StreamLink';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import { ModalSubmit } from 'components/common';
import { DEFAULT_FIELDS } from 'components/common/message/helpers';

import useMessageFavoriteFieldsForEditing from './hooks/useMessageFavoriteFieldsForEditing';
import MessageFieldsEditModeLists from './MessageFieldsEditModeLists';
import useSendFavoriteFieldTelemetry from './hooks/useSendFavoriteFieldTelemetry';

const FieldsContainer = styled.div(
  ({ theme }) => css`
    overflow: visible;
    padding: ${theme.spacings.xs} 0;
  `,
);

const ModalContentContainer = styled.div`
  font-size: ${({ theme }) => theme.fonts.size.small};
`;

const StyledAlert = styled(Alert)`
  margin-top: 0;
`;

const MessageFieldsEditModal = ({ toggleEditMode }) => {
  const { t } = useTranslation('common');
  const {
    editingFavoriteFields,
    saveEditedFavoriteFields,
    resetFavoriteFields,
    reorderFavoriteFields,
    onFavoriteToggle,
  } = useMessageFavoriteFieldsForEditing();
  const sendFavoriteFieldTelemetry = useSendFavoriteFieldTelemetry();
  const { editableStreams } = useContext(MessageFavoriteFieldsContext);

  const _saveFavoriteField = useCallback(() => {
    saveEditedFavoriteFields();
    toggleEditMode();
  }, [saveEditedFavoriteFields, toggleEditMode]);

  const _resetFavoriteFields = useCallback(() => {
    resetFavoriteFields();
    toggleEditMode();
  }, [resetFavoriteFields, toggleEditMode]);

  const _toggleEditMode = useCallback(() => {
    sendFavoriteFieldTelemetry('EDIT_CANCELED');

    return toggleEditMode();
  }, [sendFavoriteFieldTelemetry, toggleEditMode]);

  return (
    <Modal onHide={_toggleEditMode} show>
      <Modal.Header>
        <Modal.Title>{t('message.favorite_fields_configuration')}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <ModalContentContainer>
          <FieldsContainer>
            <StyledAlert bsStyle="info">
              <Trans
                t={t}
                i18nKey="message.favorite_fields_hint"
                count={editableStreams.length}
                components={{
                  streams: (
                    <span>
                      {editableStreams.map((stream, index) => {
                        const isLast = index === editableStreams.length - 1;

                        return (
                          <React.Fragment key={stream.id}>
                            <StreamLink stream={stream} />
                            {!isLast && ', '}
                          </React.Fragment>
                        );
                      })}
                    </span>
                  ),
                }}
              />
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
          submitButtonText={t('message.save_configuration')}
          leftCol={
            <Button
              bsStyle="link"
              onClick={_resetFavoriteFields}
              title={t('message.reset_to_default_title', { fields: DEFAULT_FIELDS.join(', ') })}>
              {t('message.reset_to_default')}
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
