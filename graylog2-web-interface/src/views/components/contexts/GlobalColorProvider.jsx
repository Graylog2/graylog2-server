// @flow strict
import * as React from 'react';
import { useCallback, useState } from 'react';
import chroma from 'chroma-js';
import ViewColorContext from './ViewColorContext';

const pickColor = (assignedColors: { [string]: string }) => chroma.random().toString();

type Props = {
  children: React.Node,
};
const GlobalColorProvider = ({ children }: Props) => {
  const [assignedColors, setAssignedColors] = useState({});
  const setColor = useCallback((name, color) => {
    const newAssignedColors = { ...assignedColors, [name]: color };
    setAssignedColors(newAssignedColors);
  }, [assignedColors, setAssignedColors]);
  const getColor = (name) => {
    if (assignedColors[name]) {
      return assignedColors[name];
    }
    const color = pickColor(assignedColors);

    setColor(name, color);
    return color;
  };
  return (
    <ViewColorContext.Provider value={{ getColor }}>
      {children}
    </ViewColorContext.Provider>
  );
};

export default GlobalColorProvider;
