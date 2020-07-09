// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import User from 'logic/users/User';
import { OverlayTrigger, Popover } from 'components/graylog';
import { Timestamp, Icon } from 'components/common';

type Props = {
  lastActivity: $PropertyType<User, 'lastActivity'>,
  clientAddress: $PropertyType<User, 'clientAddress'>,
  sessionActive: $PropertyType<User, 'sessionActive'>,
};

const ActiveIcon = styled(Icon)(({ theme }) => `
  color: ${theme.colors.variant.success};
`);

const LoggedInCell = ({ lastActivity, clientAddress, sessionActive }: Props) => (
  <td className="centered">
    {sessionActive && (
      <OverlayTrigger trigger={['hover', 'focus']}
                      placement="right"
                      overlay={(
                        <Popover id="session-badge-details" title="Logged in">
                          <div>Last activity: {lastActivity ? <Timestamp dateTime={lastActivity} relative /> : '-'}</div>
                          <div>Client address: {clientAddress}</div>
                        </Popover>
                      )}
                      rootClose>
        <ActiveIcon name="circle" />
      </OverlayTrigger>
    )}
  </td>
);

export default LoggedInCell;
