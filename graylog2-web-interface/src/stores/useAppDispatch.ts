import { useDispatch } from 'react-redux';

import type createStore from 'store';

export type AppDispatch = ReturnType<typeof createStore>['dispatch'];
const useAppDispatch: () => AppDispatch = useDispatch;

export default useAppDispatch;
