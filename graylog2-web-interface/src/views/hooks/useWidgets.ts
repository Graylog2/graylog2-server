import useAppSelector from 'stores/useAppSelector';
import useCurrentQueryId from 'views/logic/queries/useCurrentQueryId';

const useWidgets = () => {
  const activeQuery = useCurrentQueryId();

  return useAppSelector((state) => state?.view?.view?.state?.get(activeQuery).widgets);
};

export default useWidgets;
