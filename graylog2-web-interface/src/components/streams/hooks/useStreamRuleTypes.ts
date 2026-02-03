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
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import type { StreamRuleType } from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import { defaultOnError } from 'util/conditional/onError';

const useStreamRuleTypes = (): { data: Array<StreamRuleType> | undefined } => {
  const { data } = useQuery({
    queryKey: ['streams', 'rule-types'],

    queryFn: () =>
      defaultOnError<Array<StreamRuleType>>(
        StreamRulesStore.types(),
        'Loading stream rule types failed with status',
        'Could not load stream rule types',
      ),

    placeholderData: keepPreviousData,

    // 1 hour
    staleTime: 60 * (60 * 1000),
  });

  return { data };
};

export default useStreamRuleTypes;
