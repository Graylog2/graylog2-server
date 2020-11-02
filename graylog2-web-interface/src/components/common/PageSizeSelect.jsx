// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import { Input } from 'components/bootstrap';

const Wrapper: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 5px;

  && .form-group {
    margin-bottom: 0
  }
  .control-label {
    padding-top: 0;
  }
  .page-size-select {
    display: flex;
    align-items: baseline;
  }
`;

const PAGE_SIZES = [10, 50, 100];

type Props = {
  className?: string,
  pageSize: number,
  pageSizes: Array<number>,
  onChange: (event: SyntheticInputEvent<HTMLLinkElement>) => void,
};

const PageSizeSelect = ({ pageSizes, pageSize, onChange, className }: Props) => (
  <Wrapper className={`${className ?? ''} form-inline page-size pull-right`}>
    <Input id="page-size"
           type="select"
           bsSize="small"
           label="Show"
           value={pageSize}
           onChange={onChange}
           formGroupClassName="page-size-select">
      {pageSizes.map((size) => <option key={`option-${size}`} value={size}>{size}</option>)}
    </Input>
  </Wrapper>
);

PageSizeSelect.propTypes = {
  className: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  pageSize: PropTypes.number.isRequired,
  pageSizes: PropTypes.arrayOf(PropTypes.number),
};

PageSizeSelect.defaultProps = {
  className: '',
  pageSizes: PAGE_SIZES,
};

PageSizeSelect.defaultPageSizes = PAGE_SIZES;

export default PageSizeSelect;
