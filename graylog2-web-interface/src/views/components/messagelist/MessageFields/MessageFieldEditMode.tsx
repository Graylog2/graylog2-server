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

import type FieldType from 'views/logic/fieldtypes/FieldType';
import type { Message } from 'views/components/messagelist/Types';
import { MessageFieldName, MessageFieldValue } from 'views/components/messagelist/MessageField';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import CommonFavoriteIcon from 'views/components/common/CommonFavoriteIcon';

type Props = {
  fieldName: string;
  fieldType: FieldType;
  message: Message;
  value: any;
  isFavorite: boolean;
};

const DefinitionDescription = styled.dd(
  ({ theme }) => `
  font-family: ${theme.fonts.family.monospace};
`,
);

const FieldValueContainer = styled.div(
  () => css`
    display: flex;
    flex-direction: column;
  `,
);

const Container = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.xs};
    align-items: center;
  `,
);

const MessageFieldEditMode = ({ fieldName, fieldType, message, value, isFavorite }: Props) => {
  const { removeFromFavoriteFields, addToFavoriteFields } = useContext(MessageFavoriteFieldsContext);
  const onFavoriteToggle = useCallback(() => {
    const clb = isFavorite ? removeFromFavoriteFields : addToFavoriteFields;

    return clb(fieldName);
  }, [addToFavoriteFields, fieldName, isFavorite, removeFromFavoriteFields]);

  const favTitle = isFavorite ? `Remove ${fieldName} from favorites` : `Add ${fieldName} to favorites`;

  return (
    <Container>
      <CommonFavoriteIcon isFavorite={isFavorite} title={favTitle} onClick={onFavoriteToggle} />
      <FieldValueContainer>
        <b>
          <MessageFieldName message={message} fieldName={fieldName} fieldType={fieldType} />
        </b>
        <span>
          <DefinitionDescription data-testid={`message-field-value-${fieldName}`}>
            <MessageFieldValue value={value} fieldName={fieldName} fieldType={fieldType} message={message} />
          </DefinitionDescription>
        </span>
      </FieldValueContainer>
    </Container>
  );
};

export default MessageFieldEditMode;
