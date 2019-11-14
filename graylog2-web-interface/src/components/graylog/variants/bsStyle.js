import theme from 'styled-theming';
import { color } from 'theme';

const variantColors = {
  danger: color.secondary.uno,
  default: color.secondary.due,
  info: color.tertiary.uno,
  primary: color.tertiary.quattro,
  success: color.tertiary.tre,
  warning: color.tertiary.sei,
};
const bsStyles = Object.keys(variantColors);

const bsStyleThemeVariant = (cssBuilder, additionalVariants = {}, includedVariants = bsStyles) => {
  const variants = includedVariants.map((variant) => {
    return {
      [variant]: {
        color: cssBuilder(variantColors[variant], variant),
      },
    };
  });

  return theme.variants('mode', 'bsStyle', Object.assign(additionalVariants, ...variants));
};


export default bsStyleThemeVariant;
export { bsStyles, variantColors };
