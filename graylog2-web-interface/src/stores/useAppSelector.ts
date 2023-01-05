import { useSelector } from 'react-redux';

import type { RootState } from 'views/types';

const useAppSelector = <R, >(fn: (state: RootState) => R) => useSelector<RootState, R>(fn);
export default useAppSelector;
