import PropTypes from 'prop-types';
import React from 'react';

const AppWithExtendedSearchBar = ({ children }) => {
  return (
    <div>
      {children}
    </div>
  );
};

AppWithExtendedSearchBar.propTypes = {
  children: PropTypes.element.isRequired,
};

export default AppWithExtendedSearchBar;
