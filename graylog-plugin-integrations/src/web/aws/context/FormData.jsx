import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

export const FormDataContext = createContext();

export const FormDataProvider = ({ initialFormData, children }) => {
  const [formData, updateState] = useState(initialFormData);

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
  initialFormData: PropTypes.object,
};

FormDataProvider.defaultProps = {
  initialFormData: {},
};

export default FormDataProvider;
