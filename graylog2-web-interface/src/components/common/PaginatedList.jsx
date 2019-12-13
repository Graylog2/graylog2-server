// @flow strict
import PropTypes from 'prop-types';
import * as React from 'react';
import { Pagination } from 'components/graylog';
import { Input } from 'components/bootstrap';

const defaultPageSizes = [10, 50, 100];

type Props = {
  children: React.Node,
  onChange: (currentPage: number, pageSize: number) => void,
  activePage: number,
  pageSize: number,
  pageSizes: Array<number>,
  totalItems: number,
  showPageSizeSelect: boolean
}

type State = {
  currentPage: number,
  pageSize: number
}

/**
 * Wrapper component around an element that renders pagination
 * controls and provides a callback when the page or page size change.
 * You still need to fetch or filter the data yourself to ensure that
 * the selected page is displayed on screen.
 */
class PaginatedList extends React.Component<Props, State> {
  static propTypes = {
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


  static defaultProps = {
    activePage: 0,
    pageSizes: defaultPageSizes,
    pageSize: defaultPageSizes[0],
    showPageSizeSelect: true,
  };

  constructor(props: Props) {
    super(props);
    const { activePage, pageSize } = props;
    this.state = {
      currentPage: activePage > 0 ? activePage : 1,
      pageSize: pageSize,
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    const { pageSize, activePage } = this.props;

    if (activePage !== nextProps.activePage) {
      this.setState({ currentPage: nextProps.activePage });
    }
    if (pageSize !== nextProps.pageSize) {
      this.setState({ pageSize: nextProps.pageSize });
    }
  }

  _onChangePageSize = (event: SyntheticInputEvent<HTMLLinkElement>) => {
    const { onChange } = this.props;
    const { currentPage } = this.state;
    event.preventDefault();
    const pageSize = Number(event.target.value);
    this.setState({ pageSize: pageSize });
    onChange(currentPage, pageSize);
  };

  _onChangePage = (pageNo: number, event: MouseEvent) => {
    const { onChange } = this.props;
    const { pageSize } = this.state;
    event.preventDefault();
    this.setState({ currentPage: pageNo });
    onChange(pageNo, pageSize);
  };

  _pageSizeSelect = () => {
    const { showPageSizeSelect, pageSizes } = this.props;
    const { pageSize } = this.state;
    if (!showPageSizeSelect) {
      return null;
    }
    return (
      <div className="form-inline page-size" style={{ float: 'right' }}>
        <Input id="page-size" type="select" bsSize="small" label="Show:" value={pageSize} onChange={this._onChangePageSize}>
          {pageSizes.map(size => <option key={`option-${size}`} value={size}>{size}</option>)}
        </Input>
      </div>
    );
  };

  render() {
    const { totalItems, children } = this.props;
    const { pageSize, currentPage } = this.state;
    const numberPages = Math.ceil(totalItems / pageSize);

    return (
      <>
        {this._pageSizeSelect()}

        {children}

        <div className="text-center">
          <Pagination bsSize="small"
                      items={numberPages}
                      maxButtons={10}
                      activePage={currentPage}
                      onSelect={this._onChangePage}
                      prev
                      next
                      first
                      last />
        </div>
      </>
    );
  }
}

export default PaginatedList;
