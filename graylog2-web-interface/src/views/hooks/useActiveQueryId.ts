import useViewMetadata from 'views/hooks/useViewMetadata';

const useActiveQueryId = () => {
  const { activeQuery } = useViewMetadata();

  return activeQuery;
};

export default useActiveQueryId;
