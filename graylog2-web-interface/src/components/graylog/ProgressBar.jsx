import React from 'react';
import PropTypes from 'prop-types';
import styled, { css, keyframes } from 'styled-components';
import { transparentize } from 'polished';

import { util } from 'theme';
import teinte from 'theme/teinte';
import bsStyleThemeVariant from './variants/bsStyle';

const defaultBar = {
  animated: false,
  bsStyle: 'info',
  label: undefined,
  striped: false,
  value: 0,
};

const defaultStripColor = transparentize(0.75, teinte.primary.due);
const boxShadowColor = transparentize(0.9, teinte.primary.tre);

const progressBarVariants = color => css`
  background-color: ${color};
  color: ${util.readableColor(color)};
`;

const animatedStripes = keyframes`
  from {
    background-position: 40px 0;
  }

  to {
    background-position: 0 0;
  }
`;

const StyledProgressBar = styled.div(({ animated, striped, value }) => css`
  &&.progress-bar {
    width: ${value}%;
    ${bsStyleThemeVariant(progressBarVariants)}

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
  }
`);

const ProgressWrap = styled.div`
  height: 20px;
  margin-bottom: 20px;
  overflow: hidden;
  background-color: ${teinte.secondary.due};
  border-radius: 4px;
  box-shadow: inset 0 1px 2px ${boxShadowColor};
  display: flex;
  align-items: center;

  .progress-bar {
    height: 100%;
    font-size: 12px;
    line-height: 20px;
    text-align: center;
    box-shadow: inset 0 -1px 0 ${boxShadowColor};
    width: 0;
    transition: width 500ms ease-in-out;
  }
`;

const ProgressBar = ({ bars }) => {
  return (
    <ProgressWrap role="progressbar">
      {
        typeof bars === 'number'
          ? (
            <StyledProgressBar aria-valuenow={bars.value}
                               aria-valuemin="0"
                               aria-valuemax="100"
                               className="progress-bar"
                               {...defaultBar}
                               {...bars}>
              {bars.label}
            </StyledProgressBar>
          )
          : bars.map((bar) => {
            return (
              <StyledProgressBar aria-valuenow={bar.value}
                                 aria-valuemin="0"
                                 aria-valuemax="100"
                                 className="progress-bar"
                                 key={`progress-bar-${bar.value}-${bar.bsStyle}`}
                                 {...defaultBar}
                                 {...bar}>

                {bar.label}
              </StyledProgressBar>
            );
          })
      }
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
};

ProgressBar.defaultProps = {
  bars: defaultBar,
};

export default ProgressBar;
