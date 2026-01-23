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
import { useEffect, useMemo } from 'react';

import type { InputStates } from 'hooks/useInputsStates';

const useIsInitialUnknownInputState = (inputStates: InputStates, inputId: string) => {
  const seenInputIds = useMemo(() => new Set<string>(), []);

  useEffect(() => {
    if (!inputStates) {
      return;
    }

    Object.keys(inputStates).forEach((id) => {
      seenInputIds.add(id);
    });
  }, [inputStates, seenInputIds]);

  const hasKnownState = !!inputStates?.[inputId];

  return !hasKnownState && !seenInputIds.has(inputId);
};

export default useIsInitialUnknownInputState;
