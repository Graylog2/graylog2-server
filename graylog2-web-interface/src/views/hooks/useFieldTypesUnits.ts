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
import useFeature from 'hooks/useFeature';
import { UNIT_FEATURE_FLAG } from 'views/components/visualizations/Constants';

const useFieldTypesUnits = () => {
  const isFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const { data, isLoading } = useFieldTypes(undefined, undefined, isFeatureEnabled);

  return useMemo(() => {
    if (isLoading) return ({});

    return Object
      .fromEntries(data?.filter((ft) => ft?.unit?.isDefined).map((ft) => [ft.name, ft.unit]) ?? []);
  }, [data, isLoading]);
};

export default useFieldTypesUnits;
