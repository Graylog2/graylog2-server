import { css } from 'styled-components';

import teinte from 'theme/teinte';
import contrastingColor from 'util/contrastingColor';

const menuItemStyles = (options = {}) => {
  const { sibling = false } = options;

  return css`
  ${sibling && '& ~'} .dropdown-menu {
    > li > a {
      color: ${teinte.primary.tre};

      :hover,
      :focus {
        color: ${contrastingColor(teinte.secondary.due)};
        background-color: ${teinte.secondary.due};
      }
    }

    > .active > a {
      background-color: ${teinte.tertiary.due};
      color: ${contrastingColor(teinte.tertiary.due)};

      :hover,
      :focus {
        background-color: ${teinte.tertiary.uno};
        color: ${contrastingColor(teinte.tertiary.uno)};
      }
    }

    > .disabled > a {
      background-color: ${teinte.secondary.due};
      color: ${contrastingColor(teinte.secondary.due, 'AA')};

      :hover {
        color: ${contrastingColor(teinte.secondary.due, 'AA')};
      }
    }
  }
`;
};

export default menuItemStyles;
