import useAppSelector from 'stores/useAppSelector';
import { selectHasUndeclaredParameters } from 'views/logic/slices/searchMetadataSlice';

const useHasUndeclaredParameters = () => useAppSelector(selectHasUndeclaredParameters);

export default useHasUndeclaredParameters;
