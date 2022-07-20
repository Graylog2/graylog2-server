import { createContext, useContext } from 'react';

export const QueryValidationContext = createContext(undefined);
export const QueryValidationProvider = QueryValidationContext.Provider;

export const useQueryValidationContext = <T>() => {
  return useContext<T>(QueryValidationContext);
};
