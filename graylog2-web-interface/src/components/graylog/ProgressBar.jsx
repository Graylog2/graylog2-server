// eslint-disable-next-line no-restricted-imports
import { ProgressBar } from 'react-bootstrap';
import { css } from 'styled-components';
import { transparentize } from 'polished';

import teinte from 'theme/teinte';
import { variantColors } from './variants/bsStyle';

const defaultStripColor = transparentize(0.75, teinte.primary.due);

const variants = (styles) => {
  let style = '';

  styles.forEach((variant) => {
    style += `.progress-bar-${variant} {
      background-color: ${variantColors[variant]};
    }`;
  });

  return css`${style}`;
};

export const progressBarStyles = css`
  .progress {
    background-color: ${teinte.secondary.due};

    .progress-bar {
      color: ${teinte.primary.due};
      background-color: ${teinte.tertiary.uno};
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

export default ProgressBar;
