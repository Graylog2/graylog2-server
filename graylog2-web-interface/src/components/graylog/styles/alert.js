import theme from 'styled-theming';
import { darken, lighten } from 'polished';
import { css } from 'styled-components';

import contrastingColor from 'util/contrastingColor';
import { useTheme } from 'theme/GraylogThemeContext';

const alertStyles = () => {
  const { colors } = useTheme();

  const cssBuilder = (hex) => {
    const lightenBorder = lighten(0.30, hex);
    const borderColor = lightenBorder === '#fff' ? darken(0.08, hex) : lightenBorder;

    const lightenBackground = lighten(0.40, hex);
    const backgroundColor = lightenBackground === '#fff' ? darken(0.05, hex) : lightenBackground;

    const textColor = contrastingColor(backgroundColor);

    return css`
      background-color: ${backgroundColor};
      border-color: ${borderColor};
      color: ${textColor};
    `;
  };

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

export default alertStyles;
