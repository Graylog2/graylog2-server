import theme from 'styled-theming';
import { color, themeModes } from 'theme';

const variantColors = (mode) => {
  return {
    danger: color[mode].variant.danger,
    default: color[mode].variant.default,
    info: color[mode].variant.info,
    primary: color[mode].variant.primary,
    success: color[mode].variant.success,
    warning: color[mode].variant.warning,
  };
};

const bsStyles = Object.keys(variantColors(themeModes[0]));

const bsStyleThemeVariant = (cssBuilder, additionalVariants = {}, includedVariants = bsStyles) => {
  const styleModes = (variant) => {
    const modes = {};

    themeModes.forEach((mode) => {
      modes[mode] = cssBuilder(variantColors(mode)[variant], variant);
    });

    return modes;
  };

  const variants = includedVariants.map((variant) => ({
    [variant]: { ...styleModes(variant) },
  }));

  return theme.variants('mode', 'bsStyle', Object.assign(additionalVariants, ...variants));
};

export default bsStyleThemeVariant;
export { bsStyles, variantColors };
