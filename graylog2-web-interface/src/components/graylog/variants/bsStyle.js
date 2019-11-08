import theme from 'styled-theming';
import teinte from 'theme/teinte';

const bsStyles = ['success', 'warning', 'danger', 'info', 'default', 'primary'];

const bsStyleThemeVariant = (cssBuilder, additionalVariants = {}) => {
  return theme.variants('mode', 'bsStyle', {
    danger: {
      teinte: cssBuilder(teinte.secondary.uno),
    },
    default: {
      teinte: cssBuilder(teinte.secondary.due),
    },
    info: {
      teinte: cssBuilder(teinte.tertiary.uno),
    },
    primary: {
      teinte: cssBuilder(teinte.tertiary.quattro),
    },
    success: {
      teinte: cssBuilder(teinte.tertiary.tre),
    },
    warning: {
      teinte: cssBuilder(teinte.tertiary.sei),
    },
    ...additionalVariants,
  });
};


export default bsStyleThemeVariant;
export { bsStyles };
