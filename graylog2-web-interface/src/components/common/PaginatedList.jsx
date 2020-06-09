// @flow strict
import PropTypes from 'prop-types';
import * as React from 'react';
import { useState, useEffect } from 'react';

import { Pagination } from 'components/graylog';
import { Input } from 'components/bootstrap';
import IfInteractive from 'views/components/dashboard/IfInteractive';

const DEFAULT_PAGE_SIZES = [10, 50, 100];
const INITIAL_PAGE = 1;

type PageSizeSelectProps = {
  pageSize: number,
  pageSizes: Array<number>,
  onChange: (event: SyntheticInputEvent<HTMLLinkElement>) => void,
};

const PageSizeSelect = ({ pageSizes, pageSize, onChange }: PageSizeSelectProps) => (
  <div className="form-inline page-size" style={{ float: 'right' }}>
    <Input id="page-size" type="select" bsSize="small" label="Show:" value={pageSize} onChange={onChange}>
      {pageSizes.map((size) => <option key={`option-${size}`} value={size}>{size}</option>)}
    </Input>
  </div>
);

type Props = {
  children: React.Node,
  onChange: (currentPage: number, pageSize: number) => void,
  activePage: number,
  pageSize: number,
  pageSizes: Array<number>,
  totalItems: number,
  showPageSizeSelect: boolean,
};

/**
 * Wrapper component around an element that renders pagination
 * controls and provides a callback when the page or page size change.
 * You still need to fetch or filter the data yourself to ensure that
 * the selected page is displayed on screen.
 */
const PaginatedList = ({
  activePage,
  children,
  onChange,
  pageSize: propsPageSize,
  pageSizes,
  showPageSizeSelect,
  totalItems,
}: Props) => {
  const initialPage = activePage > 0 ? activePage : INITIAL_PAGE;
  const [pageSize, setPageSize] = useState(propsPageSize);
  const [currentPage, setCurrentPage] = useState(initialPage);
  const numberPages = Math.ceil(totalItems / pageSize);

  useEffect(() => {
    setCurrentPage(activePage);
  }, [activePage]);
  useEffect(() => {
    setPageSize(propsPageSize);
  }, [propsPageSize]);

  const _onChangePageSize = (event: SyntheticInputEvent<HTMLLinkElement>) => {
    event.preventDefault();
    const newPageSize = Number(event.target.value);
    setCurrentPage(INITIAL_PAGE);
    setPageSize(newPageSize);
    onChange(INITIAL_PAGE, newPageSize);
  };

  const _onChangePage = (pageNo: number, event: MouseEvent) => {
    event.preventDefault();
    setCurrentPage(pageNo);
    onChange(pageNo, pageSize);
  };

  return (
    <>
      {showPageSizeSelect && (
        <PageSizeSelect pageSizes={pageSizes} pageSize={pageSize} onChange={_onChangePageSize} />
      )}

      {children}

      <IfInteractive>
        <div className="text-center pagination-wrapper">
          <Pagination bsSize="small"
                      items={numberPages}
                      maxButtons={10}
                      activePage={currentPage}
                      onSelect={_onChangePage}
                      prev
                      next
                      first
                      last />
        </div>
      </IfInteractive>
    </>
  );
};

PaginatedList.propTypes = {
  /** React element containing items of the current selected page. */
  children: PropTypes.node.isRequired,
  /**
   * Function that will be called when the page changes.
   * It receives the current page and the page size as arguments.
   */
  onChange: PropTypes.func.isRequired,
  /** The active page number. If not specified the active page number will be tracked internally. */
  activePage: PropTypes.number,
  /** Number of items per page. */
  pageSize: PropTypes.number,
  /** Array of different items per page that are allowed. */
  pageSizes: PropTypes.arrayOf(PropTypes.number),
  /** Total amount of items in all pages. */
  totalItems: PropTypes.number.isRequired,
  /** Whether to show the page size selector or not. */
  showPageSizeSelect: PropTypes.bool,
};

PaginatedList.defaultProps = {
  activePage: 0,
  pageSizes: DEFAULT_PAGE_SIZES,
  pageSize: DEFAULT_PAGE_SIZES[0],
  showPageSizeSelect: true,
};

export default PaginatedList;
