import theme from 'styled-theming';
import { colors, themeModes } from 'theme';

const variantColors = (mode) => {
  return {
    danger: colors[mode].variant.danger,
    default: colors[mode].variant.default,
    info: colors[mode].variant.info,
    primary: colors[mode].variant.primary,
    success: colors[mode].variant.success,
    warning: colors[mode].variant.warning,
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
