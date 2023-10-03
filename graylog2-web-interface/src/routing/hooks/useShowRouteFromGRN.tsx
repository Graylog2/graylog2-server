import { getValuesFromGRN } from 'logic/permissions/GRN';
import useShowRouteForEntity from 'routing/hooks/useShowRouteForEntity';

const useShowRouteFromGRN = (grn: string) => {
  const { id, type } = getValuesFromGRN(grn);

  return useShowRouteForEntity(id, type);
};

export default useShowRouteFromGRN;
