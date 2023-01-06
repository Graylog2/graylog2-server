import useAppSelector from 'stores/useAppSelector';

const useIsDirty = () => useAppSelector((state) => state.view?.isDirty);

export default useIsDirty;
