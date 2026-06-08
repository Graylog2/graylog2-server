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

import PLATFORMS from './platforms';
import type { PlatformId } from './platforms';

type Props = {
  platformId: PlatformId;
  fleetName?: string;
};

// A compact, read-only recap shown once the collector is connected, replacing the
// (now pointless) interactive OS grid and fleet picker.
const Summary = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: center;
    gap: ${theme.spacings.xs};
    margin-bottom: ${theme.spacings.md};
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
  `,
);

const OnboardingSummary = ({ platformId, fleetName = undefined }: Props) => {
  const platform = PLATFORMS.find((p) => p.id === platformId);

  return (
    <Summary data-testid="onboarding-summary">
      <span>{platform?.label}</span>
      {fleetName && (
        <>
          <span>·</span>
          <span>{fleetName}</span>
        </>
      )}
    </Summary>
  );
};

export default OnboardingSummary;
