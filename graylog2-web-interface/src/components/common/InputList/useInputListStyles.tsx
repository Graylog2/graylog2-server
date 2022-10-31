import { useTheme } from 'styled-components';

import { INPUT_BORDER_RADIUS } from 'theme/constants';

const useInputListStyles = (size: 'small' | 'normal') => {
  const theme = useTheme();

  const inputListTheme = {
    ...theme,
    colors: {
      ...theme.colors,
      primary: theme.colors.input.borderFocus,
      primary75: theme.colors.variant.light.default,
      primary50: theme.colors.variant.lighter.default,
      primary25: theme.colors.variant.lightest.default,
      danger: theme.colors.variant.darker.info,
      dangerLight: theme.colors.variant.lighter.info,
      neutral0: theme.colors.input.background,
      neutral5: theme.colors.input.backgroundDisabled,
      neutral10: theme.colors.variant.lightest.info,
      neutral20: theme.colors.input.border,
      neutral30: theme.colors.gray[70],
      neutral40: theme.colors.gray[60],
      neutral50: theme.colors.gray[50],
      neutral60: theme.colors.gray[40],
      neutral70: theme.colors.gray[30],
      neutral80: theme.colors.gray[20],
      neutral90: theme.colors.gray[10],
    },
  };

  const styles = {
    valueContainer: (provided: any) => ({
      ...provided,
      padding: size === 'small' ? '0 8px' : '2px 12px',
    }),
    control: (provided: any, { isFocused }) => ({
      ...provided,
      borderWidth: isFocused ? 1 : provided.borderWidth,
      outline: isFocused ? 0 : provided.outline,
      boxShadow: isFocused ? theme.colors.input.boxShadow : null,
      ...(size === 'small' ? { minHeight: 29, height: 29 } : { minHeight: 34 }),
      borderRadius: INPUT_BORDER_RADIUS,
      alignItems: 'center',
    }),
    placeHolder: (provided: any) => ({
      ...provided,
      color: theme.colors.input.placeholder,
      lineHeight: '28px',
      fontFamily: theme.fonts.family.body,
      fontSize: theme.fonts.size.body,
      fontWeight: 400,
      whiteSpace: 'nowrap',
      textOverflow: 'ellipsis',
      overflow: 'hidden',
      maxWidth: '100%',
      paddingRight: '20px',
    }),
    multiValue: (provided: any) => ({
      ...provided,
      border: `1px solid ${theme.colors.variant.lighter.info}`,
    }),
    multiValueLabel: (provided: any) => ({
      ...provided,
      padding: '2px 5px',
      fontSize: theme.fonts.size.small,
    }),
    multiValueRemove: (provided: any) => ({
      ...provided,
      borderLeft: `1px solid ${theme.colors.variant.lighter.info}`,
      paddingLeft: '5px',
      paddingRight: '5px',
      borderRadius: '0',
    }),
  };

  return { inputListTheme, styles };
};

export default useInputListStyles;
