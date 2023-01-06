import useAppSelector from 'stores/useAppSelector';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

const useActiveViewState = () => {
  const activeQuery = useActiveQueryId();

  return useAppSelector((state) => state?.view?.view?.state?.get(activeQuery));
};

export default useActiveViewState;
