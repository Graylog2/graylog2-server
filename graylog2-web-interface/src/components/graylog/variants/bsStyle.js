import theme from 'styled-theming';

import { useTheme } from 'theme/GraylogThemeContext';

const bsStyle = (cssBuilder) => {
  const { colors, utility } = useTheme();

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

export default bsStyle;
