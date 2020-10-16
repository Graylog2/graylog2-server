// @flow strict
import { useLocation } from 'react-router-dom';
import qs from 'qs';

const useQuery = () => {
  const { search } = useLocation();

  return qs.parse(search, { ignoreQueryPrefix: true });
};

export default useQuery;
