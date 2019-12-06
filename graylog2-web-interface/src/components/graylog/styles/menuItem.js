import { css } from 'styled-components';

import { util } from 'theme';

const defaultOptions = {
  sibiling: false,
};
const menuItemStyles = (color, options = defaultOptions) => css`
  ${options.sibling ? '& ~' : '&'} .dropdown-menu {
    > li > a {
      color: ${color.global.textDefault};
      font-size: 14px;

      :hover,
      :focus {
        color: ${util.contrastingColor(color.gray[90])};
        background-color: ${color.gray[90]};
      }
    }

    > .active > a {
      color: ${util.contrastingColor(color.variant.light.info)};
      background-color: ${color.variant.light.info};

      :hover,
      :focus {
        color: ${util.contrastingColor(color.variant.info)};
        background-color: ${color.variant.info};
      }
    }

    > .disabled > a {
      color: ${util.contrastingColor(color.gray[90], 'AA')};
      background-color: ${color.gray[90]};

      :hover,
      :focus {
        color: ${util.contrastingColor(color.gray[90], 'AA')};
      }
    }
  }
`;

export default menuItemStyles;
