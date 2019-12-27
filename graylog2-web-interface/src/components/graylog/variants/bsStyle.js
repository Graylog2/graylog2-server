import theme from 'styled-theming';
import { teinte, themeModes } from 'theme';

const variantColors = () => {
  return {
    danger: teinte.secondary.uno,
    default: teinte.secondary.due,
    info: teinte.tertiary.uno,
    primary: teinte.tertiary.quattro,
    success: teinte.tertiary.tre,
    warning: teinte.tertiary.sei,
  };
};
const bsStyles = Object.keys(variantColors);

const bsStyleThemeVariant = (cssBuilder, additionalVariants = {}, includedVariants = bsStyles) => {
  const styleModes = (variant) => {
    const modes = {};

    themeModes.forEach((mode) => {
      modes[mode] = cssBuilder(variantColors()[variant], variant);
    });

    return modes;
  };

  const variants = includedVariants.map(variant => ({
    [variant]: { ...styleModes(variant) },
  }));

  return theme.variants('mode', 'bsStyle', Object.assign(additionalVariants, ...variants));
};

export default bsStyleThemeVariant;
export { bsStyles, variantColors };
