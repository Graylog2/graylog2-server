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

import { BrandIcon, Card, Icon } from 'components/common';

import PLATFORMS from './platforms';
import type { PlatformIcon } from './platforms';

const Icons = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    gap: ${theme.spacings.md};
    align-items: center;
  `,
);

const IconContainer = styled.span`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;

  /* BrandIcon has its own 20x20 container — scale it to match */
  > div {
    width: 24px;
    height: 24px;

    svg {
      width: 24px;
      height: 24px;
    }
  }
`;

const MaterialIcon = styled(Icon)`
  && {
    font-size: 24px;
  }
`;

const PlatformCard = styled(Card)`
  display: flex;
  align-items: center;
  justify-content: center;
`;

const renderPlatformIcon = (icon: PlatformIcon) => {
  if (icon.type === 'brand') {
    return <BrandIcon name={icon.name} />;
  }

  return <MaterialIcon name={icon.name} />;
};

// Static, non-interactive list of the supported collector platform icons.
const PlatformIcons = () => (
  <Icons>
    {PLATFORMS.map((platform) => (
      <PlatformCard key={platform.id} padding="sm">
        <IconContainer title={platform.label}>{renderPlatformIcon(platform.icon)}</IconContainer>
      </PlatformCard>
    ))}
  </Icons>
);

export default PlatformIcons;
