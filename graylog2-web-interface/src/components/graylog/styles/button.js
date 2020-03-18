import { css } from 'styled-components';
import { darken, lighten, getLuminance } from 'polished';

import { util } from 'theme';
import bsStyleThemeVariant from '../variants/bsStyle';

const cssBuilder = (hex, variant) => {
  const isLink = variant === 'link';

  const fontContrast = (backgroundColor) => {
    if (isLink) {
      return backgroundColor;
    }

    return util.contrastingColor(backgroundColor);
  };

  const shouldMix = (value, originalColor) => {
    if (isLink) {
      return originalColor;
    }

    const mixFunc = value < 0 ? lighten : darken;
    const absValue = Math.abs(value);

    return mixFunc(absValue, originalColor);
  };

  const linkBackground = 'transparent';
  const linkBorder = 'transparent';
  const buttonColorAdjust = getLuminance(hex) > 0.5 ? darken : lighten;

  const defaultBackground = isLink ? linkBackground : hex;
  const defaultBorder = isLink ? linkBorder : buttonColorAdjust(0.05, hex);
  const defaultColor = fontContrast(hex);

  const activeBackground = isLink ? linkBackground : buttonColorAdjust(0.10, hex);
  const activeBorder = isLink ? linkBorder : buttonColorAdjust(0.15, hex);
  const activeColor = fontContrast(buttonColorAdjust(0.10, hex));

  const disabledBackground = isLink ? linkBackground : buttonColorAdjust(0.20, hex);
  const disabledBorder = isLink ? linkBorder : buttonColorAdjust(0.15, hex);
  const disabledColor = fontContrast(buttonColorAdjust(0.20, hex));

  return css`
    background-color: ${defaultBackground};
    border-color: ${defaultBorder};
    color: ${defaultColor};
    transition: background-color 150ms ease-in-out,
      border 150ms ease-in-out,
      color 150ms ease-in-out;

    :hover {
      background-color: ${shouldMix(0.05, defaultBackground)};
      border-color: ${shouldMix(0.05, defaultBorder)};
      color: ${shouldMix(0.05, defaultColor)};
    }

    &.active {
      background-color: ${activeBackground};
      border-color: ${activeBorder};
      color: ${activeColor};

      :hover {
        background-color: ${shouldMix(0.05, activeBackground)};
        border-color: ${shouldMix(0.05, activeBorder)};
        color: ${shouldMix(0.05, activeColor)};
      }
    }

    &[disabled],
    &.disabled {
      background-color: ${disabledBackground};
      border-color: ${disabledBorder};
      color: ${disabledColor};

      :hover {
        background-color: ${shouldMix(-0.05, disabledBackground)};
        border-color: ${shouldMix(-0.05, disabledBorder)};
        color: ${shouldMix(-0.05, disabledColor)};
      }
    }
  `;
};

const buttonStyles = (color) => {
  return bsStyleThemeVariant(cssBuilder, {
    link: {
      teinte: cssBuilder(color.global.link, 'link'),
    },
    default: {
      teinte: cssBuilder(color.gray[90], 'default'),
    },
  },
  ['danger', 'info', 'primary', 'success', 'warning']);
};

export default buttonStyles;
