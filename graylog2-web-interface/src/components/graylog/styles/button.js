import { css } from 'styled-components';

import { useTheme } from 'theme/GraylogThemeContext';
import bsStyleVariant from '../variants/bsStyle';

const buttonStyles = ({ active }) => {
  const { utility } = useTheme();

  const cssBuilder = (color) => {
    const darken025 = utility.darken(color, 0.25);
    const darken050 = utility.darken(color, 0.5);
    const darken075 = utility.darken(color, 0.75);
    const darken100 = utility.darken(color, 1);
    const darken125 = utility.darken(color, 1.25);

    return css`
      background-color: ${active ? darken100 : color};
      border-color: ${active ? darken125 : darken025};

      :hover {
        background-color: ${active ? darken075 : darken050};
        border-color: ${active ? darken100 : darken075};
      }
    `;
  };

  return bsStyleVariant(cssBuilder);
};

export default buttonStyles;
