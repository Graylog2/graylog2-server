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
import { useMemo, useState } from 'react';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import { Alert } from 'components/bootstrap';
import { FULL_MESSAGE_FIELD, MESSAGE_FIELD } from 'views/Constants';
import type { Message } from 'views/components/messagelist/Types';

const MessageTerms = styled.span(
  ({ theme }) => css`
    margin-right: 8px;
    font-family: ${theme.fonts.family.monospace};
  `,
);

type MessageFieldDescriptionProps = {
  message: Message;
  fieldName: string;
  renderForDisplay: (name: string) => React.ReactElement;
  customFieldActions?: React.ReactElement;
};

const MessageFieldDescription = ({
  customFieldActions = undefined,
  fieldName,
  message,
  renderForDisplay,
}: MessageFieldDescriptionProps) => {
  const [messageTerms, setMessageTerms] = useState(Immutable.List<string>());

  const shouldShowTerms = useMemo(() => messageTerms.size !== 0, [messageTerms]);

  const formattedTerms = useMemo(
    () => messageTerms.map((term) => <MessageTerms key={term}>{term}</MessageTerms>).toArray(),
    [messageTerms],
  );

  const formattedFieldActions = useMemo(
    () => (customFieldActions ? React.cloneElement(customFieldActions, { fieldName, message }) : null),
    [customFieldActions, fieldName, message],
  );

  const className = fieldName === MESSAGE_FIELD || fieldName === FULL_MESSAGE_FIELD ? 'message-field' : '';

  return (
    <dd className={className} key={`${fieldName}dd`}>
      {formattedFieldActions}
      <div className="field-value">{renderForDisplay(fieldName)}</div>
      {shouldShowTerms && (
        <Alert bsStyle="info" onDismiss={() => setMessageTerms(Immutable.List())}>
          Field terms: &nbsp;{formattedTerms}
        </Alert>
      )}
    </dd>
  );
};

export default MessageFieldDescription;
