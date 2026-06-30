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

import { AccessibleCard, BrandIcon, Icon } from 'components/common';

import PLATFORMS from './platforms';
import type { PlatformId, PlatformIcon } from './platforms';

type Props = {
  onSelect: (platformId: PlatformId) => void;
  selectedPlatform: PlatformId | null;
  disabled: boolean;
};

const Container = styled.div(
  ({ theme }) => css`
    text-align: center;
    max-width: 700px;
    margin: ${theme.spacings.md} auto;
  `,
);

const Heading = styled.h2(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.h2};
    margin-bottom: ${theme.spacings.xs};
  `,
);

const Subtitle = styled.p(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    margin-bottom: ${theme.spacings.lg};
  `,
);

const Grid = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.md};
    justify-content: center;
    flex-wrap: wrap;
  `,
);

const PlatformCard = styled(AccessibleCard)(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: ${theme.spacings.xs};
    min-width: 110px;
    padding: ${theme.spacings.md} ${theme.spacings.lg};
    position: relative;
  `,
);

const PlatformIconContainer = styled.span`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;

  /* BrandIcon has its own 20x20 container — scale it up */
  > div {
    width: 32px;
    height: 32px;

    svg {
      width: 32px;
      height: 32px;
    }
  }
`;

const PlatformLabel = styled.span(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.body};
    font-weight: 500;
  `,
);

const CheckMark = styled.span(
  ({ theme }) => css`
    position: absolute;
    top: ${theme.spacings.xxs};
    right: ${theme.spacings.xxs};
    color: ${theme.colors.variant.success};
  `,
);

const MaterialIcon = styled(Icon)`
  && {
    font-size: 32px;
  }
`;

const renderPlatformIcon = (icon: PlatformIcon) => {
  if (icon.type === 'brand') {
    return <BrandIcon name={icon.name} />;
  }

  return <MaterialIcon name={icon.name} />;
};

const PlatformPicker = ({ onSelect, selectedPlatform, disabled }: Props) => (
  <Container>
    <Heading>Get Started with Collectors</Heading>
    <Subtitle>Deploy lightweight collectors across your infrastructure to forward logs into Graylog.</Subtitle>
    <Grid>
      {PLATFORMS.map((platform) => (
        <PlatformCard
          key={platform.id}
          title={platform.label}
          isActive={selectedPlatform === platform.id}
          onClick={disabled ? undefined : () => onSelect(platform.id)}>
          {selectedPlatform === platform.id && (
            <CheckMark>
              <Icon name="check_circle" bsStyle="success" />
            </CheckMark>
          )}
          <PlatformIconContainer>{renderPlatformIcon(platform.icon)}</PlatformIconContainer>
          <PlatformLabel>{platform.label}</PlatformLabel>
        </PlatformCard>
      ))}
    </Grid>
  </Container>
);

export default PlatformPicker;
