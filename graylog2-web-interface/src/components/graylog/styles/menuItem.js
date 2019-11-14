import { css } from 'styled-components';

import { color } from 'theme';
import { contrastingColor, readableColor } from 'theme/utils';

const menuItemStyles = (options = {}) => {
  const { sibling = false } = options;

  return css`
  ${sibling && '& ~'} .dropdown-menu {
    > li > a {
      color: ${color.global.textDefault};

      :hover,
      :focus {
        color: ${readableColor(color.gray[90])};
        background-color: ${color.gray[90]};
      }
    }

    > .active > a {
      color: ${readableColor(color.variant.light.info)};
      background-color: ${color.variant.light.info};

      :hover,
      :focus {
        color: ${readableColor(color.variant.info)};
        background-color: ${color.variant.info};
      }
    }

    > .disabled > a {
      color: ${contrastingColor(color.gray[90], 'AA')};
      background-color: ${color.gray[90]};

      :hover {
        color: ${contrastingColor(color.gray[90], 'AA')};
      }
    }
  }
`;
};

export default menuItemStyles;
