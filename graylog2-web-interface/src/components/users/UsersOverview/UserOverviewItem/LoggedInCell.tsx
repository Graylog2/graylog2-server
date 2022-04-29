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
import type { $PropertyType } from 'utility-types';

import type UserOverview from 'logic/users/UserOverview';
import { OverlayTrigger, RelativeTime } from 'components/common';
import { Popover } from 'components/bootstrap';

import LoggedInIcon from '../../LoggedInIcon';

type Props = {
  lastActivity: $PropertyType<UserOverview, 'lastActivity'>,
  clientAddress: $PropertyType<UserOverview, 'clientAddress'>,
  sessionActive: $PropertyType<UserOverview, 'sessionActive'>,
};

const Td = styled.td`
  width: 35px;
  text-align: right;
  position: relative;
`;

const LoggedInCell = ({ lastActivity, sessionActive, clientAddress }: Props) => {
  return (
    <Td>
      <OverlayTrigger trigger={['hover', 'focus']}
                      placement="right"
                      overlay={(
                        <Popover id="session-badge-details" title={sessionActive ? 'Logged in' : ''}>
                          {sessionActive ? (
                            <>
                              <div>Last activity: {lastActivity
                                ? <RelativeTime dateTime={lastActivity} /> : '-'}
                              </div>
                              <div>Client address: {clientAddress ?? '-'}</div>
                            </>
                          ) : 'Not logged in'}
                        </Popover>
                      )}
                      rootClose>
        <LoggedInIcon active={sessionActive} />
      </OverlayTrigger>
    </Td>
  );
};

export default LoggedInCell;
