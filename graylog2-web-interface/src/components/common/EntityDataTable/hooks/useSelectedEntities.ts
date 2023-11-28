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

import type { SetStateAction } from 'react';
import { useCallback, useState } from 'react';
import isFunction from 'lodash/isFunction';

const useSelectedEntities = <T>(initialSelection: Array<T>, onChangeSelection: (selectedEntities: Array<T>) => void): [Array<T>, (setSelectedEntitiesArgument: SetStateAction<Array<T>>) => void] => {
  const [selectedEntities, setSelectedEntities] = useState<Array<T>>(initialSelection ?? []);

  const _setSelectedEntities = useCallback((setSelectedEntitiesArgument: SetStateAction<Array<T>>) => {
    const newState = isFunction(setSelectedEntitiesArgument) ? setSelectedEntitiesArgument(selectedEntities) : setSelectedEntitiesArgument;

    setSelectedEntities(newState);
    if (onChangeSelection) onChangeSelection(newState);
  }, [onChangeSelection, selectedEntities]);

  return [selectedEntities, _setSelectedEntities];
};

export default useSelectedEntities;
