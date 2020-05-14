// @flow strict
import * as React from 'react';
import { useCallback, useState } from 'react';
import ViewColorContext from './ViewColorContext';

const pickColor = () => '#333';

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
    const color = pickColor();

    setColor(name, color);
    return color;
  }
  return (
    <ViewColorContext.Provider value={{ getColor }}>
      {children}
    </ViewColorContext.Provider>
  );
};

export default GlobalColorProvider;
