import * as React from 'react';
import { useRef } from 'react';
import { withAsyncPaginate } from "react-select-async-paginate";

import Select from 'components/common/Select';

const AsyncPaginateSelect = withAsyncPaginate(Select);
type Props = React.ComponentProps<typeof AsyncPaginateSelect>;

const PaginatedSelect = (props: Props) => {
  const selectRef = useRef();

  return (
    <AsyncPaginateSelect SelectComponent={Select} selectRef={selectRef} {...props} />
  );
};

PaginatedSelect.propTypes = Select.propTypes;

export default PaginatedSelect;
