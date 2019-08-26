import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

import DEFAULT_SETTINGS from './default_settings';

export const FormDataContext = createContext();

export const FormDataProvider = ({ children }) => {
  const initialState = { ...DEFAULT_SETTINGS };
  const [formData, updateState] = useState(initialState);

  const setFormData = (id, fieldData) => updateState({
    ...formData,
    [id]: {
      ...formData[id],
      ...fieldData,
    },
  });

  return (
    <FormDataContext.Provider value={{ formData, setFormData }}>
      {children}
    </FormDataContext.Provider>
  );
};

FormDataProvider.propTypes = {
  children: PropTypes.any.isRequired,
};
