import chroma from 'chroma-js';
import { css } from 'styled-components';

const buttonStyles = ({ color }) => {
  const variants = {
    danger: color.variant.danger,
    default: color.gray[90],
    info: color.variant.info,
    link: 'rgba(255, 255, 255, 0)',
    primary: color.variant.primary,
    success: color.variant.success,
    warning: color.variant.warning,
  };

  const mixColor = (originalColor) => chroma.mix(originalColor, color.global.textDefault, 0.15);

  return Object.keys(variants).map((variant) => css(({ theme }) => {
    const variantColor = variants[variant];
    const isLink = variant === 'link';

    const buttonAdjustColor = chroma(variantColor).luminance() > 0.5 ? color.global.textDefault : color.global.textAlt;

    const defaultBackground = variantColor;
    const defaultBorder = isLink ? variants.link : chroma.mix(variantColor, buttonAdjustColor, 0.05);
    const defaultColor = isLink ? color.global.link : theme.utils.contrastingColor(defaultBackground);

    const activeBackground = isLink ? variants.link : chroma.mix(variantColor, buttonAdjustColor, 0.10);
    const activeBorder = isLink ? variants.link : chroma.mix(variantColor, buttonAdjustColor, 0.15);
    const activeColor = isLink ? color.global.linkHover : theme.utils.contrastingColor(activeBackground);

    const disabledBackground = isLink ? variants.link : chroma.mix(variantColor, buttonAdjustColor, 0.20);
    const disabledBorder = isLink ? variants.link : chroma.mix(variantColor, buttonAdjustColor, 0.15);
    const disabledColor = isLink ? color.global.link : theme.utils.contrastingColor(disabledBackground, 'AA');

    return css`
      &.btn-${variant} {
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
      }
    `;
  }));
};

export default buttonStyles;
