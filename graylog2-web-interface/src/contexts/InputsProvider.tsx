import * as React from 'react';
import { useEffect } from 'react';

import type { ExtractStoreState } from 'stores/connect';
import { useStore } from 'stores/connect';
import InputsContext from 'contexts/InputsContext';
import { InputsStore, InputsActions } from 'stores/inputs/InputsStore';

const mapInputs = (state: ExtractStoreState<typeof InputsStore>) => Object.fromEntries(state?.inputs?.map((input) => [input.id, input]) ?? []);

const InputsProvider = ({ children }: React.PropsWithChildren<{}>) => {
  useEffect(() => { InputsActions.list(); }, []);
  const value = useStore(InputsStore, mapInputs);

  return (
    <InputsContext.Provider value={value}>
      {children}
    </InputsContext.Provider>
  );
};

export default InputsProvider;
