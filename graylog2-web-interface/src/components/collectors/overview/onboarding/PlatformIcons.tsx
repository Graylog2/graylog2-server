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

import { BrandIcon, Icon } from 'components/common';
import IconCard from 'components/welcome/IconCard';

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

const MaterialIcon = styled(Icon)`
  && {
    font-size: 24px;
  }
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
      <IconCard key={platform.id} title={platform.label}>{renderPlatformIcon(platform.icon)}</IconCard>
    ))}
  </Icons>
);

export default PlatformIcons;
