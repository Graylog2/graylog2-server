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

// TODO: Consider hoisting NumberCard from enterprise/src/web/security-app/components/Welcome/Grid/NumberCard.tsx
//       to components/common and using it here instead. NumberCard has more features (icon, sublabel, clickable).

import * as React from 'react';
import styled, { css } from 'styled-components';

type Variant = 'default' | 'success' | 'warning' | 'danger';

type Props = {
  value: number;
  label: string;
  variant?: Variant;
  onClick?: () => void;
};

const StyledCard = styled.div<{ $variant: Variant; $clickable: boolean }>(
  ({ theme, $variant, $clickable }) => css`
    text-align: center;
    min-width: 100px;
    padding: ${theme.spacings.md};
    background-color: ${theme.colors.cards.background};
    border: 1px solid ${theme.colors.cards.border};
    border-radius: 8px;
    font: inherit;
    color: inherit;
    width: auto;

    ${$clickable &&
    css`
      cursor: pointer;

      &:hover {
        outline: 1px solid ${theme.colors.variant.info};
      }
    `}

    ${$variant !== 'default' &&
    css`
      border-left: 3px solid ${theme.colors.variant[$variant]};
    `}
  `,
);

const Value = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.huge};
    font-weight: bold;
    line-height: 1.2;
  `,
);

const Label = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.gray[60]};
  `,
);

const StatCard = ({ value, label, variant = 'default', onClick = undefined }: Props) => (
  <StyledCard
    as={onClick ? 'button' : 'div'}
    $variant={variant}
    $clickable={!!onClick}
    onClick={onClick}
    type={onClick ? 'button' : undefined}>
    <Value>{value}</Value>
    <Label>{label}</Label>
  </StyledCard>
);

export default StatCard;
