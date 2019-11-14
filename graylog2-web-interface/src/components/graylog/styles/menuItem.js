import { css } from 'styled-components';

import { color } from 'theme';
import contrastingColor from 'util/contrastingColor';

const menuItemStyles = (options = {}) => {
  const { sibling = false } = options;

  return css`
  ${sibling && '& ~'} .dropdown-menu {
    > li > a {
      color: ${color.primary.tre};

      :hover,
      :focus {
        color: ${contrastingColor(color.secondary.due)};
        background-color: ${color.secondary.due};
      }
    }

    > .active > a {
      background-color: ${color.tertiary.due};
      color: ${contrastingColor(color.tertiary.due)};

      :hover,
      :focus {
        background-color: ${color.tertiary.uno};
        color: ${contrastingColor(color.tertiary.uno)};
      }
    }

    > .disabled > a {
      background-color: ${color.secondary.due};
      color: ${contrastingColor(color.secondary.due, 'AA')};

      :hover {
        color: ${contrastingColor(color.secondary.due, 'AA')};
      }
    }
  }
`;
};

export default menuItemStyles;
