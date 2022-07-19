import * as React from 'react';
import { useRef } from 'react';

import Select from 'components/common/Select';

type Props = React.ComponentProps<typeof Select>;

const PaginatedSelect = (props: Props) => {
  const selectRef = useRef();

  return (
    <Select ref={selectRef} async {...props} />
  );
};

PaginatedSelect.propTypes = Select.propTypes;

export default PaginatedSelect;
