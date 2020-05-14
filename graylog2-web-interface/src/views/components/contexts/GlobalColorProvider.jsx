// @flow strict
import * as React from 'react';
import { useCallback, useMemo, useState } from 'react';
import { withTheme } from 'styled-components';
import { type ThemeInterface } from 'theme';

import ViewColorContext from './ViewColorContext';

type Props = {
  children: React.Node,
  theme: ThemeInterface,
};

const scaleFromTheme = (theme) => {
  return ['', 'light', 'dark']
    .flatMap((lightness) => ['info', 'primary', 'success', 'warning', 'danger']
      .map((variant) => (lightness !== '' ? theme.color.variant[lightness][variant] : theme.color.variant[variant])));
};

const GlobalColorProvider = ({ children, theme }: Props) => {
  const [assignedColors, setAssignedColors] = useState({});
  const scale = useMemo(() => scaleFromTheme(theme), [theme]);
  const setColor = useCallback((name, color) => {
    const newAssignedColors = { ...assignedColors, [name]: color };
    setAssignedColors(newAssignedColors);
  }, [assignedColors, setAssignedColors]);
  const getColor = (name) => {
    if (assignedColors[name]) {
      return assignedColors[name];
    }
    const color = scale[Object.entries(assignedColors).length % scale.length];

    setColor(name, color);
    return color;
  };
  return (
    <ViewColorContext.Provider value={{ getColor }}>
      {children}
    </ViewColorContext.Provider>
  );
};

export default withTheme(GlobalColorProvider);
