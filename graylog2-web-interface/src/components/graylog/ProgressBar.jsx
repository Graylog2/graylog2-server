// eslint-disable-next-line no-restricted-imports
import { ProgressBar } from 'react-bootstrap';
import { css } from 'styled-components';
import { transparentize } from 'polished';

import { variantColors } from './variants/bsStyle';

const variants = (styles) => {
  let style = '';

  styles.forEach((variant) => {
    style += `.progress-bar-${variant} {
      background-color: ${variantColors[variant]};
    }`;
  });

  return css`${style}`;
};

export const progressBarStyles = css(({ theme }) => {
  const defaultStripColor = transparentize(0.75, theme.color.brand.secondary);

  return css`
    .progress {
      background-color: ${theme.color.gray[90]};

      .progress-bar {
        color: ${theme.color.gray[100]};
        background-color: ${theme.color.variant.light.info};
      }

      .progress-striped .progress-bar,
      .progress-bar-striped {
        background-image: linear-gradient(45deg,
                          ${defaultStripColor} 25%,
                          transparent 25%,
                          transparent 50%,
                          ${defaultStripColor} 50%,
                          ${defaultStripColor} 75%,
                          transparent 75%,
                          transparent);
      }

      ${variants(['success', 'info', 'warning', 'danger'])};
    }
  `;
});

export default ProgressBar;
