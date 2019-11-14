import theme from 'styled-theming';
import { color } from 'theme';

const variantColors = {
  danger: color.variant.danger,
  default: color.variant.default,
  info: color.variant.info,
  primary: color.variant.primary,
  success: color.variant.success,
  warning: color.variant.warning,
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
