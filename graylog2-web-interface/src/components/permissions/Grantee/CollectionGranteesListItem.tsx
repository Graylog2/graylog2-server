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

import type { CurrentState as CurrentGranteeState } from 'logic/permissions/SelectedGrantee';
import type SelectedGrantee from 'logic/permissions/SelectedGrantee';
import {
  GranteeListItemTitle,
  GranteeInfo,
  StyledGranteeIcon,
  GranteeListItemContainer,
} from 'components/permissions/CommonStyledComponents';

const capabilities = {
  view: 'viewer',
  manage: 'manager',
  own: 'owner',
};

type Props = {
  currentGranteeState: CurrentGranteeState;
  grantee: SelectedGrantee;
};

const CollectionGranteesListItem = ({ currentGranteeState, grantee: { capabilityId, type, title } }: Props) => (
  <GranteeListItemContainer $currentState={currentGranteeState}>
    <GranteeInfo title={title}>
      <StyledGranteeIcon type={type} />
      <GranteeListItemTitle>{title}</GranteeListItemTitle>
    </GranteeInfo>
    <GranteeInfo>collection 1</GranteeInfo>
    <GranteeInfo>{capabilities[capabilityId]}</GranteeInfo>
  </GranteeListItemContainer>
);

export default CollectionGranteesListItem;
