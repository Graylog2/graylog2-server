import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

export const FormDataContext = createContext();

export const FormDataProvider = ({ initialFormData, children }) => {
  const [formData, updateState] = useState(initialFormData);

  const setFormData = (id, fieldData) => {
    updateState({
      ...formData,
      [id]: {
        ...formData[id],
        ...fieldData,
        dirty: true,
      },
    });
  };

  const clearField = (id) => {
    if (Object.keys(formData).find(field => field === id)) {
      delete formData[id];
      updateState(formData);
    }
  };


  return (
    <FormDataContext.Provider value={{ formData, setFormData, clearField }}>
      {children}
    </FormDataContext.Provider>
  );
};

FormDataProvider.propTypes = {
  children: PropTypes.any.isRequired,
  initialFormData: PropTypes.object,
};

FormDataProvider.defaultProps = {
  initialFormData: {},
};

export default FormDataProvider;
