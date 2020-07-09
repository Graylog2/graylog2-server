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

const DetailsPopover = (
  {
    clientAddress,
    lastActivity,
  }: {
    clientAddress: $PropertyType<Props, 'clientAddress'>,
    lastActivity: $PropertyType<Props, 'lastActivity'>,
  },
) => (
  <Popover id="session-badge-details" title="Logged in">
    <div>Last activity: <Timestamp dateTime={lastActivity} relative /></div>
    <div>Client address: {clientAddress}</div>
  </Popover>
);

const LoggedInCell = ({ lastActivity, clientAddress, sessionActive }: Props) => (
  <td className="centered">
    {sessionActive && (
      <OverlayTrigger trigger={['hover', 'focus']}
                      placement="left"
                      overlay={(
                        <DetailsPopover lastActivity={lastActivity}
                                        clientAddress={clientAddress} />
                      )}
                      rootClose>
        <ActiveIcon name="circle" />
      </OverlayTrigger>
    )}
  </td>
);

export default LoggedInCell;
