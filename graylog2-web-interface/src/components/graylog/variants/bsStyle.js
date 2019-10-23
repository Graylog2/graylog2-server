import theme from 'styled-theming';

import { useTheme } from 'theme/GraylogThemeContext';

const bsStyle = (cssBuilder) => {
  const { colors } = useTheme();

  return theme.variants('mode', 'bsStyle', {
    danger: {
      teinte: cssBuilder(colors.secondary.uno),
    },
    default: {
      teinte: cssBuilder(colors.secondary.due),
    },
    info: {
      teinte: cssBuilder(colors.tertiary.uno),
    },
    primary: {
      teinte: cssBuilder(colors.tertiary.quattro),
    },
    success: {
      teinte: cssBuilder(colors.tertiary.tre),
    },
    warning: {
      teinte: cssBuilder(colors.tertiary.sei),
    },
  });
};

export default bsStyle;
