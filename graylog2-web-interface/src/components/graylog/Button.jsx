import React, { memo } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';
import chroma from 'chroma-js';
import { memoize } from 'lodash';

import { util } from 'theme';
import { propTypes, defaultProps } from './props/button';

const buttonStyles = memoize((bsStyle, color) => {
  const variants = {
    danger: color.variant.danger,
    default: color.gray[90],
    info: color.variant.info,
    link: 'rgba(255, 255, 255, 0)',
    primary: color.variant.primary,
    success: color.variant.success,
    warning: color.variant.warning,
  };

  const hex = variants[bsStyle];
  const isLink = bsStyle === 'link';

  const mixColor = memoize((originalColor) => chroma.mix(originalColor, color.global.textDefault, 0.15));

  const buttonAdjustColor = chroma(hex).luminance() > 0.5 ? color.global.textDefault : color.global.textAlt;

  const defaultBackground = hex;
  const defaultBorder = isLink ? variants.link : chroma.mix(hex, buttonAdjustColor, 0.05);
  const defaultColor = isLink ? color.global.link : util.contrastingColor(defaultBackground);

  const activeBackground = isLink ? variants.link : chroma.mix(hex, buttonAdjustColor, 0.10);
  const activeBorder = isLink ? variants.link : chroma.mix(hex, buttonAdjustColor, 0.15);
  const activeColor = isLink ? color.global.linkHover : util.contrastingColor(activeBackground);

  const disabledBackground = isLink ? variants.link : chroma.mix(hex, buttonAdjustColor, 0.20);
  const disabledBorder = isLink ? variants.link : chroma.mix(hex, buttonAdjustColor, 0.15);
  const disabledColor = isLink ? color.global.link : util.contrastingColor(disabledBackground, 'AA');

  return css`
    background-color: ${defaultBackground};
    border-color: ${defaultBorder};
    color: ${defaultColor};
    transition: background-color 150ms ease-in-out,
      border 150ms ease-in-out,
      color 150ms ease-in-out;

    :hover {
      background-color: ${mixColor(defaultBackground)};
      border-color: ${mixColor(defaultBorder)};
      color: ${mixColor(defaultColor)};
    }

    &.active {
      background-color: ${activeBackground};
      border-color: ${activeBorder};
      color: ${activeColor};

      :hover {
        background-color: ${isLink ? variants.link : mixColor(activeBackground)};
        border-color: ${mixColor(activeBorder)};
        color: ${mixColor(activeColor)};
      }
    }

    &[disabled],
    &.disabled {
      background-color: ${isLink ? variants.link : disabledBackground};
      border-color: ${disabledBorder};
      color: ${disabledColor};

      :hover {
        background-color: ${disabledBackground};
        border-color: ${disabledBorder};
        color: ${disabledColor};
      }
    }
  `;
});

const StyledButton = styled(BootstrapButton)(memoize(({ bsStyle, theme }) => css`
  ${buttonStyles(bsStyle, theme.color)}
`));

const Button = memo((props) => <StyledButton {...props} />);

Button.propTypes = propTypes;
Button.defaultProps = defaultProps;

export default Button;
export { StyledButton, buttonStyles };
