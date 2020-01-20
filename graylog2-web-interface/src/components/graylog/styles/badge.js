import theme from 'styled-theming';
import { css } from 'styled-components';

import { util } from 'theme';

const badgeStyles = () => {
  const cssBuilder = (hex) => {
    const backgroundColor = hex;
    const textColor = util.contrastingColor(backgroundColor);

    return css`
      background-color: ${backgroundColor};
      color: ${textColor};
    `;
  };

  return theme.variants('mode', 'bsStyle', {
    // NOTE: using the existing colors from previous custom styles to keep consistency while we migrate to Themeable components
    danger: {
      teinte: cssBuilder('#FF3B00'),
    },
    default: {
      teinte: cssBuilder('#AAAAAA'),
    },
    info: {
      teinte: cssBuilder('#16ACE3'),
    },
    primary: {
      teinte: cssBuilder('#9E1F63'),
    },
    success: {
      teinte: cssBuilder('#8DC63F'),
    },
    warning: {
      teinte: cssBuilder('#F7941E'),
    },
  });
};

export default badgeStyles;
