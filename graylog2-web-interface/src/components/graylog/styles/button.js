import { css } from 'styled-components';
import { darken, lighten, getLuminance } from 'polished';

import teinte from 'theme/teinte';
import contrastingColor from 'util/contrastingColor';
import bsStyleThemeVariant from '../variants/bsStyle';

const buttonStyles = ({ bsStyle }) => {
  const cssBuilder = (color) => {
    const isLink = bsStyle === 'link';

    const fontContrast = (backgroundColor) => {
      if (isLink) {
        return backgroundColor;
      }

      return contrastingColor(backgroundColor);
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
    const buttonColorAdjust = getLuminance(color) > 0.5 ? darken : lighten;

    const defaultBackground = isLink ? linkBackground : color;
    const defaultBorder = isLink ? linkBorder : buttonColorAdjust(0.05, color);
    const defaultColor = fontContrast(color);

    const activeBackground = isLink ? linkBackground : buttonColorAdjust(0.10, color);
    const activeBorder = isLink ? linkBorder : buttonColorAdjust(0.15, color);
    const activeColor = fontContrast(buttonColorAdjust(0.10, color));

    const disabledBackground = isLink ? linkBackground : buttonColorAdjust(0.20, color);
    const disabledBorder = isLink ? linkBorder : buttonColorAdjust(0.15, color);
    const disabledColor = fontContrast(buttonColorAdjust(0.20, color));

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

  return bsStyleThemeVariant(cssBuilder, {
    link: {
      teinte: cssBuilder(teinte.tertiary.quattro),
    },
  });
};

export default buttonStyles;
