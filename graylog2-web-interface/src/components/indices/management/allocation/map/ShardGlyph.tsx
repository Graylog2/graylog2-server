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
import styled, { css, useTheme } from 'styled-components';

import { copyLabel } from './mapLayout';

import type { MapCopy } from '../../types';

const SIZE = 40;

const GlyphButton = styled.button(
  ({ theme }) => css`
    border: 0;
    padding: 0;
    background: none;
    line-height: 0;
    cursor: pointer;

    &:focus-visible {
      outline: 2px solid ${theme.colors.variant.primary};
      outline-offset: 2px;
    }
  `,
);

// A shard copy as a log seen end-on (bark ring, growth rings), one log of its index's woodpile.
type Props = {
  copy: MapCopy;
  selected: boolean;
  onClick: () => void;
};

const ShardGlyph = React.forwardRef<HTMLButtonElement, Props>(({ copy, selected, onClick }: Props, ref) => {
  const theme = useTheme();
  const unassigned = copy.state === 'UNASSIGNED';
  // Unassigned primaries make an index red, unassigned replicas yellow; moving or starting copies blue; healthy green.
  const [fill, outline] = (() => {
    if (unassigned) {
      return copy.primary
        ? [theme.colors.variant.danger, theme.colors.variant.darker.danger]
        : [theme.colors.variant.warning, theme.colors.variant.darker.warning];
    }

    return copy.state === 'STARTED'
      ? [theme.colors.variant.success, theme.colors.variant.darker.success]
      : [theme.colors.variant.info, theme.colors.variant.darker.info];
  })();
  const text = theme.colors.global.textAlt;
  const label = copyLabel(copy);

  return (
    <GlyphButton ref={ref} type="button" aria-label={label} title={label} aria-pressed={selected} onClick={onClick}>
      <svg width={SIZE} height={SIZE} viewBox={`-2 -2 ${SIZE + 4} ${SIZE + 4}`} aria-hidden>
        {/* The bark: a thick ring in the darker shade; the open log gets a dark one. */}
        <circle
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={SIZE / 2 - 1.5}
          fill={fill}
          // Unassigned but resolving on its own (waiting for a node, throttled): paler.
          fillOpacity={unassigned && !copy.needs_action ? 0.55 : 1}
          stroke={selected ? theme.colors.text.primary : outline}
          strokeWidth={3}
        />
        {/* Growth rings, faint so the label stays readable. */}
        <circle cx={SIZE / 2} cy={SIZE / 2} r={SIZE / 2 - 7} fill="none" stroke={outline} strokeOpacity={0.35} />
        <circle cx={SIZE / 2} cy={SIZE / 2} r={SIZE / 2 - 12} fill="none" stroke={outline} strokeOpacity={0.35} />
        <text
          x={SIZE / 2}
          y={SIZE / 2 + 1}
          textAnchor="middle"
          dominantBaseline="middle"
          fontSize="16"
          fontWeight="bold"
          fill={text}>
          {copy.shard}
          <tspan fontSize="11" dx="1">
            {copy.primary ? 'P' : 'R'}
          </tspan>
        </text>
      </svg>
    </GlyphButton>
  );
});

ShardGlyph.displayName = 'ShardGlyph';

export default ShardGlyph;
