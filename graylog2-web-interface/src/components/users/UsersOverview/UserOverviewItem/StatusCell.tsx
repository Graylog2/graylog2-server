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
import { OverlayTrigger, Icon } from 'components/common';
import { Popover } from 'components/bootstrap';

type Props = {
  accountStatus: $PropertyType<UserOverview, 'accountStatus'>,
};

const Wrapper = styled.div<{ enabled: boolean }>(({ theme, enabled }) => `
  color: ${enabled ? theme.colors.variant.success : theme.colors.variant.default};
`);

const Td = styled.td`
  width: 35px;
  text-align: center;
`;

const StatusCell = ({ accountStatus }: Props) => (
  <Td>
    <OverlayTrigger trigger={['hover', 'focus']}
                    placement="right"
                    overlay={(
                      <Popover id="session-badge-details">
                        {`User is ${accountStatus}`}
                      </Popover>
                    )}
                    rootClose>
      <Wrapper enabled={accountStatus === 'enabled'}>
        <Icon name={accountStatus === 'enabled' ? 'check-circle' : 'times-circle'} />
      </Wrapper>
    </OverlayTrigger>
  </Td>
);

export default StatusCell;
