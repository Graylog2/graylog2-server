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
import type { PropsWithChildren } from 'react';
import styled, { css } from 'styled-components';

import { Link } from 'components/common/router';
import { NAV_ITEM_HEIGHT } from 'theme/constants';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';

const BrandContainer = styled.div`
  display: flex;
  align-items: center;
`;

const BrandLink = styled(Link)(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    min-height: ${NAV_ITEM_HEIGHT};
    color: ${theme.colors.global.textDefault};
    &:hover,
    &:active,
    &:focus {
      text-decoration: none;
      color: ${theme.colors.global.textDefault};
    }
  `,
);

const ActivePerspectiveBrand = ({
  children = undefined,
  className = '',
}: PropsWithChildren<{ className?: string }>) => {
  const { activePerspective } = useActivePerspective();
  const ActiveBrandComponent = activePerspective?.brandComponent;

  if (!ActiveBrandComponent) {
    return null;
  }

  return (
    <BrandContainer className={className}>
      <BrandLink to={activePerspective.welcomeRoute}>
        <ActiveBrandComponent />
      </BrandLink>
      {children}
    </BrandContainer>
  );
};

export default ActivePerspectiveBrand;
