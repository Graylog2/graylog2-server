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
import { useMemo } from 'react';

import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';

const useFieldTypesUnits = () => {
  const { data, isLoading } = useFieldTypes(undefined, undefined);

  return useMemo(() => {
    if (isLoading) return {};

    return data
      .filter((ft) => ft?.unit?.isDefined)
      .reduce((res, ft) => {
        res[ft.name] = ft.unit;

        return res;
      }, {});
  }, [data, isLoading]);
};

export default useFieldTypesUnits;
