// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Input } from 'components/bootstrap';

const PAGE_SIZES = [10, 50, 100];

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

PageSizeSelect.propTypes = {
  className: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  pageSize: PropTypes.number.isRequired,
  pageSizes: PropTypes.arrayOf(PropTypes.number),
};

PageSizeSelect.defaultProps = {
  className: undefined,
  pageSizes: PAGE_SIZES,
};

PageSizeSelect.defaultPageSizes = PAGE_SIZES;

export default PageSizeSelect;
