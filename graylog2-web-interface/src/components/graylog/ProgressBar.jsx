// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { css, keyframes } from 'styled-components';
import { transparentize } from 'polished';

import { util } from 'theme';
import teinte from 'theme/teinte';
import bsStyleThemeVariant from './variants/bsStyle';

type Props = {
  bars: {
    animated: boolean,
    bsStyle: string,
    label: string,
    striped: boolean,
    value: number,
   },
  className: string,
};

const DEFAULT_BAR = {
  animated: false,
  bsStyle: 'info',
  label: undefined,
  striped: false,
  value: 0,
};

const defaultStripColor = transparentize(0.75, teinte.primary.due);
const boxShadowColor = transparentize(0.9, teinte.primary.tre);

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

const ProgressWrap = styled.div`
  height: 20px;
  margin-bottom: 20px;
  overflow: hidden;
  background-color: ${teinte.secondary.due};
  border-radius: 4px;
  box-shadow: inset 0 1px 2px ${boxShadowColor};
  display: flex;
  align-items: center;
`;

const Bar = styled(({ animated, bsStyle, striped, value, ...rest }) => <div {...rest} />)(props => css`
  height: 100%;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  box-shadow: inset 0 -1px 0 ${boxShadowColor};
  transition: width 500ms ease-in-out;
  width: ${props.value}%;
  max-width: 100%;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4), 2px -1px 3px rgba(255, 255, 255, 0.5);
  ${(props.animated || props.striped) && css`
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
  ${props.animated && css`
    animation: ${animatedStripes} 2s linear infinite;
  `}
  ${bsStyleThemeVariant(progressBarVariants)}
`);

const ProgressBar = ({ bars, className }: Props) => {
  return (
    <ProgressWrap role="progressbar" className={className}>
      {bars.map((bar, index) => {
        const { label, ...rest } = bar;

        return (
          <Bar aria-valuenow={rest.value}
               aria-valuemin="0"
               aria-valuemax="100"
               aria-valuetext={label}
               key={`bar-${index}`} // eslint-disable-line react/no-array-index-key
               {...DEFAULT_BAR}
               {...rest}>
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
