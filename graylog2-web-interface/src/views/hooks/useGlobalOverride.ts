import useAppSelector from 'stores/useAppSelector';
import { selectGlobalOverride } from 'views/logic/slices/searchExecutionSlice';

const useGlobalOverride = () => useAppSelector(selectGlobalOverride);

export default useGlobalOverride;
