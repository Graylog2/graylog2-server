import { useDispatch } from 'react-redux';

import type createStore from 'store';

type AppDispatch = ReturnType<typeof createStore>['dispatch'];
const useAppDispatch: () => AppDispatch = useDispatch;

export default useAppDispatch;
