import { css } from 'styled-components';
import theme from 'styled-theming';

const buttonStyles = ({ colors, utility, specific = true }) => {
  const cssBuilder = (color) => {
    return css`
      ${specific && '&& {'}
        background-color: ${color};
        border-color: ${utility.darken(color, 0.25)};

        :hover {
          background-color: ${utility.darken(color, 0.25)};
          border-color: ${utility.darken(color, 0.5)};
        }
        ${specific && '}'}
    `;
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
