import * as React from 'react';
import { useContext } from 'react';
import type { SelectProps } from '@mantine/core';
import { Select as MantineSelect } from '@mantine/core';
import { ThemeContext } from 'styled-components';

const Select = ({ children, ...otherProps }: SelectProps) => {
  const theme = useContext(ThemeContext);
  const SelectStyles = () => ({
    input: {
      color: theme.colors.input.color,
      backgroundColor: theme.colors.input.background,
      borderColor: theme.colors.input.border,
    },
    dropdown: {
      color: theme.colors.input.color,
      backgroundColor: theme.colors.input.background,
    },
  });

  return (
    <MantineSelect {...otherProps}
                   styles={SelectStyles}>
      {children}
    </MantineSelect>
  );
};

export default Select;
