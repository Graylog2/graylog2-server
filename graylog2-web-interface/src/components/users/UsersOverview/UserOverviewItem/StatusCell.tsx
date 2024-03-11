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
import type { $PropertyType } from 'utility-types';

import type UserOverview from 'logic/users/UserOverview';
import { Icon } from 'components/common';
import Tooltip from 'components/common/Tooltip';

type Props = {
  authServiceEnabled: $PropertyType<UserOverview, 'authServiceEnabled'>,
  accountStatus: $PropertyType<UserOverview, 'accountStatus'>,
};

const Wrapper = styled.div<{ $enabled: boolean }>(({ theme, $enabled }) => css`
  color: ${$enabled ? theme.colors.variant.success : theme.colors.variant.default};
`);

const Td = styled.td`
  width: 35px;
  text-align: center;
`;

const StatusCell = ({ accountStatus, authServiceEnabled }: Props) => (
  <Td>
    <Tooltip withArrow position="right" label={<>{`User is ${accountStatus}`}{!authServiceEnabled ? ' (authentication service is disabled)' : ''}</>}>
      <Wrapper $enabled={authServiceEnabled && accountStatus === 'enabled'}>
        <Icon name={accountStatus === 'enabled' ? 'check_circle' : 'cancel'} />
      </Wrapper>
    </Tooltip>
  </Td>
);

export default StatusCell;
