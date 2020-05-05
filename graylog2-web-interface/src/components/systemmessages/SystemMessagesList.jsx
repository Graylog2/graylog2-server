import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Table } from 'components/graylog';
import { SystemMessage } from 'components/systemmessages';

const SystemMessagesTable = styled(Table)(({ theme }) => `
  font-size: 14px;

  a {
    color: ${theme.color.global.textDefault};
  }
`);

const TimestampTH = styled.th`
  width: 200px;
`;

const SystemMessagesList = ({ messages }) => {
  return (
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
};

SystemMessagesList.propTypes = {
  messages: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default SystemMessagesList;
