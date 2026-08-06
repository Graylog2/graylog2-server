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
import type { ColorVariant } from '@graylog/sawmill';

import { HoverForHelp, AccessibleCard } from 'components/common';
export type Variant = 'default' | 'success' | 'warning' | 'danger' | 'primary';

type Props = {
  value: number | React.ReactNode;
  label: string | React.ReactNode;
  subValue?: string | React.ReactNode;
  helpText?: React.ReactNode;
  variant?: ColorVariant;
  onClick?: () => void;
  className?: string;
};

const Card = styled(AccessibleCard)<{ $variant: ColorVariant }>(
  ({ theme, $variant }) => css`
    text-align: center;
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
    min-width: 100px;
    padding: ${theme.spacings.md};
    color: inherit;
    font: inherit;
    ${$variant !== 'default' &&
    css`
      border-left: 3px solid ${theme.colors.variant[$variant]};
    `}
    position: relative;
  `,
);

const HelpCorner = styled.div(
  ({ theme }) => css`
    position: absolute;
    top: ${theme.spacings.xs};
    right: ${theme.spacings.xs};
    color: ${theme.colors.text.secondary};
    font-size: 0.7em;
  `,
);

const CardLabel = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
    font-weight: 600;
  `,
);

const CardValue = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.text.primary};
    font-size: ${theme.fonts.size.huge};
    font-weight: 700;
    line-height: 1.2;
    overflow-wrap: anywhere;
  `,
);

const CardSubValue = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
  `,
);

const StatCard = ({
  value,
  label,
  subValue = null,
  helpText = undefined,
  variant = 'default',
  onClick = undefined,
  className = undefined,
}: Props) => (
  <Card $variant={variant} onClick={onClick} className={className}>
    {helpText && (
      <HelpCorner>
        <HoverForHelp title={label} triggerTitle="More info" placement="right" pullRight={false}>
          {helpText}
        </HoverForHelp>
      </HelpCorner>
    )}
    <CardLabel>{label}</CardLabel>
    <CardValue>{value}</CardValue>
    <CardSubValue>{subValue}</CardSubValue>
  </Card>
);

export default StatCard;
