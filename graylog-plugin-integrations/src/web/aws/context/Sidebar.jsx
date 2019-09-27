import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

export const SidebarContext = createContext();

export const SidebarProvider = ({ children }) => {
  const [sidebar, setSidebar] = useState(<></>);

  const clearSidebar = () => {
    setSidebar(<></>);
  };

  return (
    <SidebarContext.Provider value={{ sidebar, clearSidebar, setSidebar }}>
      {children}
    </SidebarContext.Provider>
  );
};

SidebarProvider.propTypes = {
  children: PropTypes.any.isRequired,
};
