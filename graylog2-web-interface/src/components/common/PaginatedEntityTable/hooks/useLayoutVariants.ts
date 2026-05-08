import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type {TimeRange} from 'views/logic/queries/Query';

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

type LayoutVariant = {
  displayName: string;
  entityListId: string;
  layoutVariant: string;
  metrics?: Array<{
    meaning: string;
    metricName: string;
    value: number;
  }>;
};

const url = ({ entityListId }: Props): string => qualifyUrl(`/entitylists/preferences/list_predefined/${entityListId}`);

const fetchLayoutVariants = (props: Props): Promise<Array<LayoutVariant>> =>
  fetch('POST', url(props), props.timerange).then((response: Array<LayoutVariantJSON>) =>
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
