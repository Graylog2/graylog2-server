import useAppSelector from 'stores/useAppSelector';

const useView = () => useAppSelector((state) => state?.view?.view);

export default useView;
