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
import { useRef, useState, useEffect } from 'react';
import debounce from 'lodash/debounce';

import Select from 'components/common/Select';
import { Spinner } from 'components/common';

const DEFAULT_PAGINATION = { page: 1, perPage: 50, query: '' };

type Pagination = {
  page: number,
  perPage: number,
  query: string,
}

type PaginatedOptions = {
  total: number,
  pagination: Pagination,
  list: Array<{ label: string, value: unknown }>,
}

type Props = Omit<React.ComponentProps<typeof Select>, 'options'> & {
  onLoadOptions: (pagination: Pagination) => Promise<PaginatedOptions>,
}

const PaginatedSelect = ({ onLoadOptions, ...rest }: Props) => {
  const selectRef = useRef();
  const [paginatedOptions, setPaginatedOptions] = useState<PaginatedOptions | undefined>();
  const [isSearching, setIsSearching] = useState(false);

  const handleSearch = debounce((newValue, actionMeta) => {
    if (actionMeta.action === 'input-change') {
      setIsSearching(true);

      onLoadOptions({ ...DEFAULT_PAGINATION, query: newValue }).then((results) => {
        setIsSearching(false);
        setPaginatedOptions(results);
      });
    } else if (actionMeta.action === 'menu-close') {
      onLoadOptions(DEFAULT_PAGINATION).then(setPaginatedOptions);
    }
  }, 400);

  const handleLoadMore = debounce(() => {
    const { pagination, total, list } = paginatedOptions;

    if (isSearching) {
      return;
    }

    if (total > list.length) {
      onLoadOptions({ ...pagination, page: pagination.page + 1, query: '' }).then((res) => setPaginatedOptions((cur) => ({
        ...res,
        list: [...cur.list, ...res.list],
      })));
    }
  }, 400);

  useEffect(() => {
    if (!paginatedOptions) {
      onLoadOptions(DEFAULT_PAGINATION).then(setPaginatedOptions);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!paginatedOptions) {
    return <Spinner text="Loading options..." />;
  }

  return (
    <Select {...rest}
            ref={selectRef}
            options={paginatedOptions.list}
            onInputChange={handleSearch}
            loadOptions={handleLoadMore}
            async
            total={paginatedOptions.total} />
  );
};

export default PaginatedSelect;
