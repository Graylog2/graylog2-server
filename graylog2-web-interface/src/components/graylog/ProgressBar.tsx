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
import PropTypes from 'prop-types';
import styled, { css, keyframes } from 'styled-components';
import chroma from 'chroma-js';

type StyledBarProps = {
  animated: boolean,
  bsStyle: string,
  striped: boolean,
  value: number,
};

type BarProps = StyledBarProps & {
  label: string,
};

type ProgressBarProps = {
  bars: Array<BarProps>,
  className: string,
};

const DEFAULT_BAR = {
  animated: false,
  bsStyle: 'info',
  label: undefined,
  striped: false,
  value: 0,
};

const boxShadow = (meta) => css`
  box-shadow: ${meta} ${({ theme }) => chroma(theme.colors.brand.secondary).alpha(0.1).css()};
`;

const animatedStripes = keyframes`
  from {
    background-position: 40px 0;
  }

  to {
    background-position: 0 0;
  }
`;

const progressBarVariants = css(({ bsStyle, theme }) => {
  if (!bsStyle) {
    return undefined;
  }

  return `
    background-color: ${theme.colors.variant[bsStyle]};
    color: ${theme.utils.readableColor(theme.colors.variant[bsStyle])};
  `;
});

const ProgressWrap = styled.div(({ theme }) => css`
  height: 20px;
  margin-bottom: 20px;
  overflow: hidden;
  background-color: ${theme.colors.gray[90]};
  border-radius: 4px;
  ${boxShadow('inset 0 1px 2px')};
  display: flex;
  align-items: center;
`);

const Bar = styled.div<StyledBarProps>(({ animated, striped, theme, value }) => {
  const defaultStripColor = chroma(theme.colors.global.contentBackground).alpha(0.25).css();

  return css`
    height: 100%;
    font-size: ${theme.fonts.size.small};
    line-height: 20px;
    text-align: center;
    ${boxShadow('inset 0 -1px 0')};
    transition: width 500ms ease-in-out;
    width: ${value}%;
    max-width: 100%;
    text-shadow: 0 1px 2px ${chroma(theme.colors.gray[10]).alpha(0.4).css()}, 2px -1px 3px ${chroma(theme.colors.gray[100]).alpha(0.5).css()};
    ${(animated || striped) && css`
      background-image: linear-gradient(
        45deg,
        ${defaultStripColor} 25%,
        transparent 25%,
        transparent 50%,
        ${defaultStripColor} 50%,
        ${defaultStripColor} 75%,
        transparent 75%,
        transparent
      );
      background-size: 40px 40px;
    `}
    ${animated && css`
      animation: ${animatedStripes} 2s linear infinite;
    `}
    ${progressBarVariants}
  `;
});

const ProgressBar = ({ bars, className }: ProgressBarProps) => {
  return (
    <ProgressWrap className={className}>
      {bars.map((bar, index) => {
        const { label, animated, bsStyle, striped, value } = { ...DEFAULT_BAR, ...bar };

        return (
          <Bar role="progressbar"
               aria-valuenow={value}
               aria-valuemin="0"
               aria-valuemax="100"
               aria-valuetext={label}
               key={`bar-${index}`} // eslint-disable-line react/no-array-index-key
               animated={animated}
               bsStyle={bsStyle}
               striped={striped}
               value={value}>
            {label}
          </Bar>
        );
      })}
    </ProgressWrap>
  );
};

ProgressBar.propTypes = {
  bars: PropTypes.arrayOf(PropTypes.shape({
    animated: PropTypes.bool,
    bsStyle: PropTypes.string,
    label: PropTypes.string,
    striped: PropTypes.bool,
    value: PropTypes.number,
  })),
  className: PropTypes.string,
};

ProgressBar.defaultProps = {
  bars: [DEFAULT_BAR],
  className: undefined,
};

export default ProgressBar;
export { Bar };
