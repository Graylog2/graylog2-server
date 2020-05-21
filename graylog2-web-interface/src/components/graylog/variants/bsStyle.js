import theme from 'styled-theming';
import { colors, themeModes } from 'theme';

const variantColors = () => {
  return {
    danger: colors.variant.danger,
    default: colors.variant.default,
    info: colors.variant.info,
    primary: colors.variant.primary,
    success: colors.variant.success,
    warning: colors.variant.warning,
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
