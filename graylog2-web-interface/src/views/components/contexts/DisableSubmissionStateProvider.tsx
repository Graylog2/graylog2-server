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
import { useCallback, useState, useMemo } from 'react';

import DisableSubmissionStateContext from './DisableSubmissionStateContext';

type Props = {
  children: React.ReactElement,
};

const DisableSubmissionStateProvider = ({ children }: Props) => {
  const [isDisabledMap, setIsDisabledMap] = useState({});

  const setDisabled = useCallback((sectionKey, disabled) => {
    if (Boolean(isDisabledMap[sectionKey]) !== disabled) {
      setIsDisabledMap({
        ...isDisabledMap,
        [sectionKey]: disabled,
      });
    }
  }, [isDisabledMap]);
  const disabled = Object.values(isDisabledMap).includes(true);
  const contextValue = useMemo(() => ({ disabled, setDisabled }), [disabled, setDisabled]);

  return (
    <DisableSubmissionStateContext.Provider value={contextValue}>
      {children}
    </DisableSubmissionStateContext.Provider>
  );
};

export default DisableSubmissionStateProvider;
