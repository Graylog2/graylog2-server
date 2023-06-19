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
