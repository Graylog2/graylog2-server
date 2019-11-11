import theme from 'styled-theming';
import teinte from 'theme/teinte';

const variantColors = {
  danger: teinte.secondary.uno,
  default: teinte.secondary.due,
  info: teinte.tertiary.uno,
  primary: teinte.tertiary.quattro,
  success: teinte.tertiary.tre,
  warning: teinte.tertiary.sei,
};
const bsStyles = Object.keys(variantColors);

const bsStyleThemeVariant = (cssBuilder, additionalVariants = {}, includedVariants = bsStyles) => {
  const variants = includedVariants.map((variant) => {
    return {
      [variant]: {
        teinte: cssBuilder(variantColors[variant], variant),
      },
    };
  });

  return theme.variants('mode', 'bsStyle', Object.assign(additionalVariants, ...variants));
};


export default bsStyleThemeVariant;
export { bsStyles, variantColors };
