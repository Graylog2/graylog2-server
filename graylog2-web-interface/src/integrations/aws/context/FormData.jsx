/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
    if (Object.keys(formData).find((field) => field === id)) {
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
