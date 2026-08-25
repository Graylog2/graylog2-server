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

import type { BsSize } from 'components/bootstrap/types';

import Badge from './Badge';

const StyledBadge = styled(Badge)(
  ({ theme }) => css`
    border-radius: 3px;
    font-weight: normal;
    padding-left: ${theme.spacings.xs};
    padding-right: ${theme.spacings.xs};
    text-align: center;

    .mantine-Badge-label {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: ${theme.spacings.xxs};
    }

    /* When a label's content is wrapped in a <span> (e.g. to carry a title tooltip), that span
       becomes a flex item of the label, so it needs to be its own single-line ellipsis context —
       otherwise long text overflows instead of truncating when the label is width-constrained. */
    .mantine-Badge-label > span {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  `,
);

type Props = React.PropsWithChildren<{
  'aria-label'?: string;
  /** @deprecated Legacy size alias — prefer bsSize on new call sites too, this component only supports the legacy shape. */
  bsSize?: BsSize;
  bsStyle?: ColorVariant;
  className?: string;
  'data-testid'?: string;
  onClick?: (e: React.MouseEvent) => void;
  onMouseEnter?: React.MouseEventHandler<HTMLElement>;
  onMouseLeave?: React.MouseEventHandler<HTMLElement>;
  role?: string;
  style?: React.CSSProperties;
  title?: string;
  uppercase?: boolean;
}>;

/** Plain legacy status label — small pill shape, bsStyle-only. Use Badge for the color/variant/dot API. */
const Label = ({
  'aria-label': ariaLabel = undefined,
  bsSize = undefined,
  bsStyle = undefined,
  className = undefined,
  children = undefined,
  'data-testid': dataTestid = undefined,
  onClick = undefined,
  onMouseEnter = undefined,
  onMouseLeave = undefined,
  role = undefined,
  style = undefined,
  title = undefined,
  uppercase = false,
}: Props) => (
  <StyledBadge
    aria-label={ariaLabel}
    bsSize={bsSize}
    bsStyle={bsStyle}
    className={className}
    data-testid={dataTestid}
    onClick={onClick}
    onMouseEnter={onMouseEnter}
    onMouseLeave={onMouseLeave}
    role={role}
    style={style}
    title={title}
    uppercase={uppercase}>
    {children}
  </StyledBadge>
);

export default Label;
