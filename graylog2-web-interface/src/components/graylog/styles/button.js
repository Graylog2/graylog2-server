import { css } from 'styled-components';
import chroma from 'chroma-js';

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
    const absValue = Math.abs(value);
    const mixColor = absValue < 0 ? '#fff' : '#000';

    return isLink ? originalColor : chroma.mix(originalColor, mixColor, absValue);
  };

  const linkBackground = 'transparent';
  const linkBorder = 'transparent';
  const buttonAdjustColor = chroma(hex).luminance() > 0.5 ? '#000' : '#fff';

  const defaultBackground = isLink ? linkBackground : hex;
  const defaultBorder = isLink ? linkBorder : chroma.mix(hex, buttonAdjustColor, 0.05);
  const defaultColor = fontContrast(hex);

  const activeBackground = isLink ? linkBackground : chroma.mix(hex, buttonAdjustColor, 0.10);
  const activeBorder = isLink ? linkBorder : chroma.mix(hex, buttonAdjustColor, 0.15);
  const activeColor = fontContrast(chroma.mix(hex, buttonAdjustColor, 0.10));

  const disabledBackground = isLink ? linkBackground : chroma.mix(hex, buttonAdjustColor, 0.20);
  const disabledBorder = isLink ? linkBorder : chroma.mix(hex, buttonAdjustColor, 0.15);
  const disabledColor = fontContrast(chroma.mix(hex, buttonAdjustColor, 0.20));

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
