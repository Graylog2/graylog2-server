import * as React from 'react';
import { createContext, useCallback, useMemo, useState } from 'react';

import type { FormFieldDataType, FormDataType, FormDataContextType } from '../utils/types';

type SetFormDataType = (id: string, fieldData: FormFieldDataType) => void;
type ClearFieldType = (id: string) => void;

interface Props {
  initialFormData?: FormDataType;
  children: React.ReactNode;
}

export const FormDataContext = createContext<FormDataContextType>({
  formData: {},
  setFormData: () => {},
  clearField: () => {},
});

export const FormDataProvider = ({ initialFormData = {}, children }: Props) => {
  const [formData, updateState] = useState<FormDataType>(initialFormData);

  const setFormData: SetFormDataType = useCallback(
    (id, fieldData) => {
      updateState({
        ...formData,
        [id]: {
          ...formData[id],
          ...fieldData,
          dirty: true,
        },
      });
    },
    [formData],
  );

  const clearField: ClearFieldType = useCallback(
    (id) => {
      if (Object.keys(formData).find((field) => field === id)) {
        delete formData[id];
        updateState(formData);
      }
    },
    [formData],
  );

  const formdatacontextprovider = useMemo(
    () => ({ formData, setFormData, clearField }),
    [formData, setFormData, clearField],
  );

  return <FormDataContext.Provider value={formdatacontextprovider}>{children}</FormDataContext.Provider>;
};

export default FormDataProvider;
