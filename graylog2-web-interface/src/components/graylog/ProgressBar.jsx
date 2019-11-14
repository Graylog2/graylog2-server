// eslint-disable-next-line no-restricted-imports
<<<<<<< HEAD
import { ProgressBar } from 'react-bootstrap';
import { css } from 'styled-components';

import teinte from 'theme/teinte';
=======
import { ProgressBar as BootstrapProgressBar } from 'react-bootstrap';
import { css, createGlobalStyle } from 'styled-components';
>>>>>>> a8b3deb61... Organizing Theme Files
import { transparentize } from 'polished';

import { color } from 'theme';
import { variantColors } from './variants/bsStyle';

const defaultStripColor = transparentize(0.75, color.primary.due);

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
    background-color: ${color.secondary.due};

    .progress-bar {
      color: ${color.primary.due};
      background-color: ${color.tertiary.uno};
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
