/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';

import { useErrorsContext } from 'components/lookup-tables/contexts/ErrorsContext';
import { useFetchErrors } from 'components/lookup-tables/hooks/useLookupTablesAPI';

const ErrorsConsumer = ({
  lutNames = undefined,
  cacheNames = undefined,
  adapterNames = undefined,
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
          ({ tables, caches, data_adapters }: { tables: unknown; caches: unknown; data_adapters: unknown }) =>
            setErrors({ lutErrors: tables, cacheErrors: caches, adapterErrors: data_adapters }),
        );
      }, 1000),
    );

    return () => clearInterval(fetchInterval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lutNames, cacheNames, adapterNames]);

  return null;
};

export default ErrorsConsumer;
