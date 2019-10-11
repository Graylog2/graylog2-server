import React, { createContext, useContext, useState } from 'react';
import PropTypes from 'prop-types';

const defaultValues = { fields: {}, allFields: {} };
const FieldListContext = createContext(defaultValues);
export const useFieldList = () => useContext(FieldListContext);

const FieldListProvider = ({ children }) => {
  const [filter, setFilter] = useState('');
  const [showFieldsBy, setShowFieldsBy] = useState('current');

  const resetFilter = () => {
    setFilter('');
  };

  return (
    <FieldListContext.Provider value={{ filter, setFilter, resetFilter, showFieldsBy, setShowFieldsBy }}>
      {children}
    </FieldListContext.Provider>
  );
};

FieldListProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default FieldListProvider;
