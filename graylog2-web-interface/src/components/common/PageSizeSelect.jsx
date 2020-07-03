// @flow strict
import * as React from 'react';

import { Input } from 'components/bootstrap';

type Props = {
  className?: string,
  pageSize: number,
  pageSizes: Array<number>,
  onChange: (event: SyntheticInputEvent<HTMLLinkElement>) => void,
};

const PageSizeSelect = ({ pageSizes, pageSize, onChange, className }: Props) => (
  <div className={`${className ?? ''} form-inline page-size`} style={{ float: 'right' }}>
    <Input id="page-size" type="select" bsSize="small" label="Show:" value={pageSize} onChange={onChange}>
      {pageSizes.map((size) => <option key={`option-${size}`} value={size}>{size}</option>)}
    </Input>
  </div>
);

PageSizeSelect.defaultProps = {
  className: undefined,
};

export default PageSizeSelect;
