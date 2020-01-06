import { css } from 'styled-components';

import teinte from 'theme/teinte';
import { util } from 'theme';

const menuItemStyles = (options = {}) => {
  const { sibling = false } = options;

  return css`
  ${sibling && '& ~'} .dropdown-menu {
    > li > a {
      color: ${teinte.primary.tre};

      :hover,
      :focus {
        color: ${util.contrastingColor(teinte.secondary.due)};
        background-color: ${teinte.secondary.due};
      }
    }

    > .active > a {
      background-color: ${teinte.tertiary.due};
      color: ${util.contrastingColor(teinte.tertiary.due)};

      :hover,
      :focus {
        background-color: ${teinte.tertiary.uno};
        color: ${util.contrastingColor(teinte.tertiary.uno)};
      }
    }

    > .disabled > a {
      color: ${util.contrastingColor(teinte.primary.tre, 'AA')};

      :hover,
      :focus {
        background-color: ${teinte.secondary.due};
        color: ${util.contrastingColor(teinte.primary.tre, 'AAA')};
      }
    }
  }
`;
};

export default menuItemStyles;
