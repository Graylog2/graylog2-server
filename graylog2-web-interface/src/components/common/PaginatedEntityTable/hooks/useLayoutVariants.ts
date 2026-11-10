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

import { useQuery } from '@tanstack/react-query';

import { EntityLists } from '@graylog/server-api';

import type { TimeRange } from 'views/logic/queries/Query';

type Props = {
  entityListId: string;
  timerange: TimeRange;
};

type LayoutVariantJSON = {
  display_name: string;
  entity_list_id: string;
  layout_variant: string;
  metrics?: Array<{
    meaning: string;
    metric_name: string;
    value: number;
  }>;
};

export type LayoutVariant<T = string> = {
  displayName: string;
  entityListId: string;
  layoutVariant: T;
  metrics?: Array<{
    meaning: string;
    metricName: string;
    value: number;
  }>;
};

const fetchLayoutVariants = ({ entityListId, timerange }: Props): Promise<Array<LayoutVariant>> =>
  EntityLists.listPredefined(entityListId, timerange).then((response: Array<LayoutVariantJSON>) =>
    response.map(({ display_name, layout_variant, entity_list_id, metrics }) => ({
      displayName: display_name,
      entityListId: entity_list_id,
      layoutVariant: layout_variant,
      metrics: metrics?.map(({ value, metric_name, meaning }) => ({
        metricName: metric_name,
        meaning,
        value,
      })),
    })),
  );

const useLayoutVariants = (props: Props) => {
  const { data = [], isFetching } = useQuery({
    queryKey: ['layout-variants', props],
    queryFn: () => fetchLayoutVariants(props),
  });

  return {
    data,
    isFetching,
  };
};

export default useLayoutVariants;
