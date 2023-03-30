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
import styled from 'styled-components';

import type { Message } from 'views/components/messagelist/Types';
import { MESSAGE_FIELD } from 'views/Constants';
import type FieldType from 'views/logic/fieldtypes/FieldType';

import CustomHighlighting from './CustomHighlighting';
import DecoratedValue from './decoration/DecoratedValue';

import TypeSpecificValue from '../TypeSpecificValue';

export const MessageWrapper = styled.div`
  white-space: pre-line;
  max-height: 6em; /* show 4 lines: line-height * 4 */
  overflow: hidden;
`;

type Props = {
  message: Message,
  messageFieldType: FieldType,
};

const MessageFieldRow = ({ message, messageFieldType }: Props) => (
  <MessageWrapper>
    <CustomHighlighting field="message"
                        value={message.fields[MESSAGE_FIELD]}>
      <TypeSpecificValue field="message"
                         value={message.fields[MESSAGE_FIELD]}
                         type={messageFieldType}
                         render={DecoratedValue} />
    </CustomHighlighting>
  </MessageWrapper>
);

export default MessageFieldRow;
