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

const LoggedInBadge = ({ lastActivity, clientAddress, sessionActive }: Props) => {
  const popover = (
    <Popover id="session-badge-details" title="Logged in">
      <div>Last activity: <Timestamp dateTime={lastActivity} relative /></div>
      <div>Client address: {clientAddress}</div>
    </Popover>
  );

  return (
    <td className="centered">
      {sessionActive && (
        <OverlayTrigger trigger={['hover', 'focus']} placement="left" overlay={popover} rootClose>
          <ActiveIcon name="circle" />
        </OverlayTrigger>
      )}
    </td>
  );
};

export default LoggedInBadge;
