import React from 'react';
import styled, { css } from 'styled-components';

import { Table } from 'components/bootstrap';
import { SystemMessage } from 'components/systemmessages';

const SystemMessagesTable = styled(Table)(({ theme }) => css`
  font-size: ${theme.fonts.size.body};

  a {
    color: ${theme.colors.global.textDefault};
  }
`);

const TimestampTH = styled.th`
  width: 200px;
`;

type SystemMessagesListProps = {
  messages: any[];
};

const SystemMessagesList = ({
  messages
}: SystemMessagesListProps) => (
  <SystemMessagesTable striped hover condensed>
    <thead>
      <tr>
        <TimestampTH>Timestamp</TimestampTH>
        <th>Node</th>
        <th>Message</th>
      </tr>
    </thead>

    <tbody>
      {messages.map((message) => <SystemMessage key={`message-${Math.random().toString(36).substring(7)}`} message={message} />)}
    </tbody>
  </SystemMessagesTable>
);

export default SystemMessagesList;
