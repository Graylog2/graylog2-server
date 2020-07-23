import React from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';

const buttonStyles = (bsStyle = 'default') => css(({ theme }) => {
  const isLink = bsStyle === 'link';

  const defaultBackground = theme.colors.variant.light[bsStyle];
  const defaultBorder = isLink ? 'rgba(0,0,0,0)' : theme.colors.variant[bsStyle];
  const defaultColor = isLink ? theme.colors.global.link : theme.utils.contrastingColor(defaultBackground);

  const defaultBackgroundHover = theme.colors.variant.lighter[bsStyle];
  const defaultBorderHover = isLink ? 'rgba(0,0,0,0)' : theme.colors.variant.light[bsStyle];
  const defaultColorHover = isLink ? theme.colors.global.link : theme.utils.contrastingColor(defaultBackgroundHover);

  const activeBackground = isLink ? 'rgba(0,0,0,0)' : theme.colors.variant[bsStyle];
  const activeBorder = isLink ? 'rgba(0,0,0,0)' : theme.colors.variant.dark[bsStyle];
  const activeColor = isLink ? theme.colors.global.linkHover : theme.utils.contrastingColor(activeBackground);

  const activeBackgroundHover = isLink ? 'rgba(0,0,0,0)' : theme.colors.variant.dark[bsStyle];
  const activeBorderHover = isLink ? 'rgba(0,0,0,0)' : theme.colors.variant.darker[bsStyle];
  const activeColorHover = isLink ? theme.colors.global.linkHover : theme.utils.contrastingColor(activeBackgroundHover);

  const disabledBackground = isLink ? 'rgba(0,0,0,0)' : theme.colors.variant.lighter[bsStyle];
  const disabledBorder = isLink ? 'rgba(0,0,0,0)' : theme.colors.variant.light[bsStyle];
  const disabledColor = isLink ? theme.colors.variant.darkest.primary : theme.utils.contrastingColor(disabledBackground);

  return css`
    &.btn-${bsStyle} {
      background-color: ${defaultBackground};
      border-color: ${defaultBorder};
      color: ${defaultColor};
      transition: background-color 150ms ease-in-out,
        border 150ms ease-in-out,
        color 150ms ease-in-out;

      :hover {
        background-color: ${defaultBackgroundHover};
        border-color: ${defaultBorderHover};
        color: ${defaultColorHover};
      }

      &.active {
        background-color: ${activeBackground};
        border-color: ${activeBorder};
        color: ${activeColor};

        :hover {
          background-color: ${activeBackgroundHover};
          border-color: ${activeBorderHover};
          color: ${activeColorHover};
        }
      }

      &[disabled],
      &.disabled {
        background-color: ${disabledBackground};
        border-color: ${disabledBorder};
        color: ${disabledColor};
        cursor: not-allowed;

        :hover {
          background-color: ${disabledBackground};
          border-color: ${disabledBorder};
          color: ${disabledColor};
        }
      }
    }
  `;
});

const Button = React.memo(styled(BootstrapButton)(({ bsStyle }) => css`
  ${buttonStyles(bsStyle)}
`));

export default Button;
export { buttonStyles };
