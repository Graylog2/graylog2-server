import { css } from 'styled-components';
import theme from 'styled-theming';

const buttonStyles = ({ colors, active, utility }) => {
  const cssBuilder = (color) => {
    const darken025 = utility.darken(color, 0.25);
    const darken050 = utility.darken(color, 0.5);
    const darken075 = utility.darken(color, 0.75);
    const darken100 = utility.darken(color, 1);
    const darken125 = utility.darken(color, 1.25);

    return css`
    && {
      background-color: ${active ? darken100 : color};
      border-color: ${active ? darken125 : darken025};

      :hover {
        background-color: ${active ? darken075 : darken050};
        border-color: ${active ? darken100 : darken075};
      }
    }`;
  };

  return theme.variants('mode', 'bsStyle', {
    danger: {
      teinte: cssBuilder(colors.secondary.uno),
      noire: cssBuilder(utility.opposite(colors.secondary.uno)),
    },
    default: {
      teinte: cssBuilder(colors.secondary.due),
      noire: cssBuilder(utility.opposite(colors.secondary.due)),
    },
    info: {
      teinte: cssBuilder(colors.tertiary.uno),
      noire: cssBuilder(utility.opposite(colors.tertiary.uno)),
    },
    primary: {
      teinte: cssBuilder(colors.tertiary.quattro),
      noire: cssBuilder(utility.opposite(colors.tertiary.quattro)),
    },
    success: {
      teinte: cssBuilder(colors.tertiary.tre),
      noire: cssBuilder(utility.opposite(colors.tertiary.tre)),
    },
    warning: {
      teinte: cssBuilder(colors.tertiary.sei),
      noire: cssBuilder(utility.opposite(colors.tertiary.sei)),
    },
  });
};

export default buttonStyles;
