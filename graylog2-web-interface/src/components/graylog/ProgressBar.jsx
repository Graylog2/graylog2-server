// @flow
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { css, keyframes, type StyledComponent } from 'styled-components';
// $FlowFixMe removing in future iteration
import { transparentize } from 'polished';

import { util, type ThemeInterface } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

type ProgressBarProps = {
  bars: Array<{
    animated: boolean,
    bsStyle: string,
    label: string,
    striped: boolean,
    value: number,
  }>,
  className: string,
};

type BarProps = {
  animated: boolean,
  striped: boolean,
  value: number,
};

const DEFAULT_BAR = {
  animated: false,
  bsStyle: 'info',
  label: undefined,
  striped: false,
  value: 0,
};

const boxShadow = meta => css`
  box-shadow: ${meta} ${({ theme }) => transparentize(0.9, theme.color.brand.secondary)};
`;

const animatedStripes = keyframes`
  from {
    background-position: 40px 0;
  }

  to {
    background-position: 0 0;
  }
`;

const progressBarVariants = color => css`
  background-color: ${color};
  color: ${util.readableColor(color)};
`;

const ProgressWrap: StyledComponent<{}, ThemeInterface, *> = styled.div(({ theme }) => css`
  height: 20px;
  margin-bottom: 20px;
  overflow: hidden;
  background-color: ${theme.color.gray[90]};
  border-radius: 4px;
  ${boxShadow('inset 0 1px 2px')};
  display: flex;
  align-items: center;
`);

const Bar: StyledComponent<BarProps, ThemeInterface, *> = styled.div(({ animated, striped, theme, value }) => {
  const defaultStripColor = transparentize(0.75, theme.color.global.contentBackground);

  return css`
    height: 100%;
    font-size: 12px;
    line-height: 20px;
    text-align: center;
    ${boxShadow('inset 0 -1px 0')};
    transition: width 500ms ease-in-out;
    width: ${value}%;
    max-width: 100%;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4), 2px -1px 3px rgba(255, 255, 255, 0.5);
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
    ${bsStyleThemeVariant(progressBarVariants)}
  `;
});

const ProgressBar = ({ bars, className }: ProgressBarProps) => {
  return (
    <ProgressWrap className={className}>
      {bars.map((bar, index) => {
        const { label, animated, bsStyle, striped, value } = bar;

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
