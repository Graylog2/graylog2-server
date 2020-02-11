import React from 'react';
import PropTypes from 'prop-types';
import styled, { css, keyframes } from 'styled-components';
import { transparentize } from 'polished';

import teinte from 'theme/teinte';
import bsStyleThemeVariant from './variants/bsStyle';

const defaultStripColor = transparentize(0.75, teinte.primary.due);
const boxShadowColor = transparentize(0.9, teinte.primary.tre);

const progressBarVariants = color => css`
  background-color: ${color};
`;

const animatedStripes = keyframes`
  from {
    background-position: 40px 0;
  }

  to {
    background-position: 0 0;
  }
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

const StyledProgressBar = styled.div(({ animated, value, striped }) => css`
  width: ${value}%;
  height: 100%;
  font-size: 12px;
  line-height: 20px;
  color: ${teinte.primary.due};
  text-align: center;
  background-color: ${teinte.tertiary.uno};
  box-shadow: inset 0 -1px 0 ${boxShadowColor};
  transition: width .6s ease;
  background-image: ${animated || striped
    ? `linear-gradient(
        45deg,
        ${defaultStripColor} 25%,
        transparent 25%,
        transparent 50%,
        ${defaultStripColor} 50%,
        ${defaultStripColor} 75%,
        transparent 75%,
        transparent
      )`
    : 'none'};
  background-size: 40px 40px;
  ${animated && css`animation: ${animatedStripes} 2s linear infinite;`}
  ${bsStyleThemeVariant(progressBarVariants, {}, ['success', 'info', 'warning', 'danger'])}
`);

const ProgressBar = ({ bars }) => {
  return (
    <ProgressWrap role="progressbar">
      {
        typeof bars === 'number'
          ? (
            <StyledProgressBar value={bars}
                               aria-valuenow={bars}
                               aria-valuemin="0"
                               aria-valuemax="100"
                               bsStyle="info" />
          )
          : bars.map((bar) => {
            return (
              <StyledProgressBar value={bar.value}
                                 aria-valuenow={bar.value}
                                 aria-valuemin="0"
                                 aria-valuemax="100"
                                 bsStyle={bar.bsStyle || 'info'}
                                 striped={bar.striped}
                                 animated={bar.animated} />
            );
          })
      }
    </ProgressWrap>
  );
};

ProgressBar.propTypes = {
  bars: PropTypes.oneOf([
    PropTypes.number,
    PropTypes.arrayOf(PropTypes.shape({
      value: PropTypes.number.isRequired,
      bsStyle: PropTypes.string,
      label: PropTypes.string,
      striped: PropTypes.bool,
      animated: PropTypes.bool,
    })),
  ]).isRequired,
};

export default ProgressBar;
