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
import { useEffect, useRef } from 'react';

import useAppSelector from 'stores/useAppSelector';
import { selectSearchJobId } from 'views/logic/slices/searchExecutionSelectors';

const useOnSearchExecution = (fn: () => void) => {
  const searchResultId = useAppSelector(selectSearchJobId);
  const lastSearchResultId = useRef<string | undefined>();

  useEffect(() => {
    if (lastSearchResultId.current !== searchResultId) {
      fn();
    }

    lastSearchResultId.current = searchResultId;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchResultId]);
};

export default useOnSearchExecution;
