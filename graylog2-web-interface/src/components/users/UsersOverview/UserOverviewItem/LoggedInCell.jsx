// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import UserOverview from 'logic/users/UserOverview';
import type { ThemeInterface } from 'theme';
import { OverlayTrigger, Popover } from 'components/graylog';
import { Timestamp } from 'components/common';

import LoggedInIcon from '../../LoggedInIcon';

type Props = {
  lastActivity: $PropertyType<UserOverview, 'lastActivity'>,
  sessionActive: $PropertyType<UserOverview, 'sessionActive'>,
};

const Td: StyledComponent<{}, ThemeInterface, HTMLTableCellElement> = styled.td`
  width: 35px;
  text-align: right;
`;

const LoggedInInfo = ({ lastActivity }: { lastActivity: $PropertyType<UserOverview, 'lastActivity'> }) => (
  <OverlayTrigger trigger={['hover', 'focus']}
                  placement="right"
                  overlay={(
                    <Popover id="session-badge-details" title="Logged in">
                      <div>Last activity: {lastActivity ? <Timestamp dateTime={lastActivity} relative /> : '-'}</div>
                    </Popover>
                  )}
                  rootClose>
    <LoggedInIcon active />
  </OverlayTrigger>
);

const LoggedInCell = ({ lastActivity, sessionActive }: Props) => (
  <Td>
    {sessionActive ? <LoggedInInfo lastActivity={lastActivity} /> : <LoggedInIcon active={false} />}
  </Td>
);

export default LoggedInCell;
