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
import React from 'react';
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import { Table } from 'components/bootstrap';
import { SystemMessage } from 'components/systemmessages';

const SystemMessagesTable = styled(Table)(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.body};

    a {
      color: ${theme.colors.text.primary};
    }
  `,
);

const TimestampTH = styled.th`
  width: 200px;
`;

type SystemMessagesListProps = {
  messages: any[];
};

const SystemMessagesList = ({ messages }: SystemMessagesListProps) => {
  const { t } = useTranslation('system');

  return (
    <SystemMessagesTable condensed>
      <thead>
        <tr>
          <TimestampTH>{t('system_messages_list.timestamp')}</TimestampTH>
          <th>{t('system_messages_list.node')}</th>
          <th>{t('system_messages_list.message')}</th>
        </tr>
      </thead>

      <tbody>
        {messages.map((message) => (
          <SystemMessage key={`message-${Math.random().toString(36).substring(7)}`} message={message} />
        ))}
      </tbody>
    </SystemMessagesTable>
  );
};

export default SystemMessagesList;
