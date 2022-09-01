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
import styled, { css } from 'styled-components';

import type MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import type { Message } from 'views/components/messagelist/Types';
import usePluginEntities from 'hooks/usePluginEntities';
import MessageFieldRow from 'views/components/messagelist/MessageFieldRow';

const TableRow = styled.tr(({ theme }) => css`
  && {
    margin-bottom: 5px;
    cursor: pointer;
  
    td {
      border-top: 0;
      padding-top: 0;
      padding-bottom: 5px;
      font-family: ${theme.fonts.family.monospace};
      color: ${theme.colors.variant.dark.info};
    }
  }
`);

const renderMessageFieldRow = (message, messageFieldType) => (
  <MessageFieldRow message={message}
                   messageFieldType={messageFieldType} />
);

type Props = {
  onRowClick: () => void,
  colSpanFixup: number,
  message: Message,
  showMessageRow?: boolean,
  messageFieldType: FieldType,
  config: MessagesWidgetConfig,
};

const MessagePreview = ({ onRowClick, colSpanFixup, message, messageFieldType, showMessageRow, config }: Props) => {
  const MessageRowOverride = usePluginEntities('views.components.widgets.messageTable.messageRowOverride')?.[0];

  return (
    <TableRow onClick={onRowClick}>
      <td colSpan={colSpanFixup}>
        {showMessageRow && !!MessageRowOverride && (
          <MessageRowOverride messageFields={message.fields}
                              config={config}
                              renderMessageRow={() => renderMessageFieldRow(message, messageFieldType)} />
        )}
        {(showMessageRow && !MessageRowOverride) && renderMessageFieldRow(message, messageFieldType)}
      </td>
    </TableRow>
  );
};

MessagePreview.defaultProps = {
  showMessageRow: false,
};

export default MessagePreview;
