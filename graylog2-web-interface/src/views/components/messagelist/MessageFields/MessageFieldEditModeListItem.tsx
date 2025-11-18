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

import React, { useCallback, useContext } from 'react';
import styled, { css } from 'styled-components';

import type FieldType from 'views/logic/fieldtypes/FieldType';
import { MessageFieldName, MessageFieldValue } from 'views/components/messagelist/MessageField';
import CommonFavoriteIcon from 'views/components/common/CommonFavoriteIcon';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';

type Props = {
  fieldName: string;
  fieldType: FieldType;
  value: any;
  isFavorite: boolean;
  onFavoriteToggle: (field: string) => void;
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

const MessageFieldEditModeListItem = ({ fieldName, fieldType, value, isFavorite, onFavoriteToggle }: Props) => {
  const favTitle = isFavorite ? `Remove ${fieldName} from favorites` : `Add ${fieldName} to favorites`;
  const { message } = useContext(MessageFavoriteFieldsContext);

  const _onFavoriteToggle = useCallback(() => onFavoriteToggle(fieldName), [fieldName, onFavoriteToggle]);

  return (
    <Container>
      <CommonFavoriteIcon isFavorite={isFavorite} title={favTitle} onClick={_onFavoriteToggle} />
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

export default MessageFieldEditModeListItem;
