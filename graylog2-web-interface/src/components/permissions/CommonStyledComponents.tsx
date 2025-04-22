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
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

import type { CurrentState as CurrentGranteeState } from 'logic/permissions/SelectedGrantee';
import { Button } from 'components/bootstrap';
import { Select } from 'components/common';

import CapabilitySelect from './CapabilitySelect';
import GranteeIcon from './GranteeIcon';

export const currentStateColor = (theme: DefaultTheme, currentState: CurrentGranteeState) => {
  switch (currentState) {
    case 'new':
      return theme.colors.variant.lighter.success;
    case 'changed':
      return theme.colors.variant.lighter.warning;
    default:
      return 'transparent';
  }
};
export const GranteeListItemContainer = styled.li<{ $currentState: CurrentGranteeState }>(
  ({ theme, $currentState }) => css`
    display: flex;
    align-items: center;
    width: 100%;
    padding: 5px;
    border-left: 5px solid ${currentStateColor(theme, $currentState)};
  `,
);

export const GranteeInfo = styled.div`
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
  margin-right: 10px;
`;

export const GranteeListItemTitle = styled.div`
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const StyledCapabilitySelect = styled(CapabilitySelect)`
  flex: 0.5;
`;

export const StyledGranteeIcon = styled(GranteeIcon)`
  margin-right: 5px;
`;

export const GranteeListItemActions = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 25px;
  margin-left: 10px;
`;
export const ShareFormSection = styled.div`
  margin-bottom: 25px;

  &:last-child {
    margin-bottom: 0;
  }
`;
export const GranteesSelectorHeadline = styled.h5`
  margin-bottom: 10px;
`;
export const ShareFormElements = styled.div`
  display: flex;
`;
export const ShareSubmitButton = styled(Button)`
  margin-left: 15px;
`;
export const GranteesSelect = styled(Select)`
  flex: 1;
`;
export const GranteesSelectOption = styled.div`
  display: flex;
  align-items: center;
`;
