import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

type Props = {
  entityListId: string;
  timerange: {
    from: string;
    to: string;
  };
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

const url = ({ entityListId, timerange: { from, to } }: Props): string => {
  const params = `?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`;

  return qualifyUrl(`/plugins/org.graylog.aws/entitylists/preferences/list_predefined/${entityListId}${params}`);
};

const fetchLayoutVariants = (props: Props): Promise<Array<LayoutVariant>> =>
  fetch('GET', url(props)).then((response: Array<LayoutVariantJSON>) =>
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
