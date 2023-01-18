import useAppSelector from 'stores/useAppSelector';
import { selectSearchExecutionResult } from 'views/logic/slices/searchExecutionSelectors';

const useSearchResult = () => useAppSelector(selectSearchExecutionResult);
export default useSearchResult;
