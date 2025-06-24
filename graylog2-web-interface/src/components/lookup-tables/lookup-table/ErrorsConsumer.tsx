import * as React from 'node_modules/@types/react';
import { useErrorsContext } from '../contexts/ErrorsContext';
import { useFetchErrors } from '../hooks/useLookupTablesAPI';

export const ErrorsConsumer = ({
  lutNames = undefined, cacheNames = undefined, adapterNames = undefined,
}: {
  lutNames?: Array<string>;
  cacheNames?: Array<string>;
  adapterNames?: Array<string>;
}) => {
  const [fetchInterval, setFetchInterval] = React.useState<NodeJS.Timeout>();
  const { setErrors } = useErrorsContext();
  const { fetchErrors } = useFetchErrors();

  React.useEffect(() => {
    if (fetchInterval) clearInterval(fetchInterval);

    setFetchInterval(
      setInterval(() => {
        fetchErrors({ lutNames, cacheNames, adapterNames }).then(
          ({ tables, caches, data_adapters }: { tables: unknown; caches: unknown; data_adapters: unknown; }) => setErrors({ lutErrors: tables, cacheErrors: caches, adapterErrors: data_adapters })
        );
      }, 10000)
    );

    return () => clearInterval(fetchInterval);
  }, []);

  return null;
};

