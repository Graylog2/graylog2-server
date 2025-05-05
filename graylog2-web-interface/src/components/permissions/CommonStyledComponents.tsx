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
import EntityCreateCapabilitySelect from './EntityCreateCapabilitySelect';

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
    padding: ${theme.spacings.xxs};
    border-left: ${theme.spacings.xxs} solid ${currentStateColor(theme, $currentState)};
  `,
);

export const GranteeInfo = styled.div(({ theme }) => css`
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
  margin-right:  ${theme.spacings.xs};
`);

export const GranteeListItemTitle = styled.div`
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const StyledCapabilitySelect = styled(CapabilitySelect)`
  flex: 0.5;
`;

export const StyledEntityCreateCapabilitySelect = styled(EntityCreateCapabilitySelect)`
  flex: 0.5;
`;

export const StyledGranteeIcon = styled(GranteeIcon)(({ theme }) => css`
  margin-right: ${theme.spacings.xxs};
`);

export const GranteeListItemActions = styled.div(({ theme }) => css`
  display: flex;
  align-items: center;
  justify-content: center;
  width: ${theme.spacings.sm};
  margin-left: ${theme.spacings.xs};
`);

export const ShareFormSection = styled.div(({ theme }) => css`
  margin-bottom: ${theme.spacings.sm};

  &:last-child {
    margin-bottom: 0;
  }
`);

export const GranteesSelectorHeadline = styled.h5(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
`);

export const ShareFormElements = styled.div`
  display: flex;
`;

export const ShareSubmitButton = styled(Button)(({ theme }) => css`
  margin-left: ${theme.spacings.xs};
`);

export const GranteesSelect = styled(Select)`
  flex: 1;
`;

export const GranteesSelectOption = styled.div`
  display: flex;
  align-items: center;
`;
