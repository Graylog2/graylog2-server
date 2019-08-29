import PropTypes from 'prop-types';
import React from 'react';
import { Pagination } from 'react-bootstrap';
import { Input } from 'components/bootstrap';

const defaultPageSizes = [10, 50, 100];

/**
 * Wrapper component around an element that renders pagination
 * controls and provides a callback when the page or page size change.
 * You still need to fetch or filter the data yourself to ensure that
 * the selected page is displayed on screen.
 */
class PaginatedList extends React.Component {
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

  state = {
    currentPage: this._defaultActivePage(),
    pageSize: this._defaultPageSize(),
  };

  componentWillReceiveProps(nextProps) {
    const { pageSize, activePage } = this.props;

    if (activePage !== nextProps.activePage) {
      this.setState({ currentPage: nextProps.activePage });
    }
    if (pageSize !== nextProps.pageSize) {
      this.setState({ pageSize: nextProps.pageSize });
    }
  }

  _defaultPageSize = () => {
    const { pageSize } = this.props;

    return pageSize;
  };

  _defaultActivePage = () => {
    const { activePage } = this.props;

    return activePage > 0 ? activePage : 1;
  }

  _onChangePageSize = (event) => {
    const { onChange } = this.props;
    const { currentPage } = this.state;

    event.preventDefault();
    const pageSize = Number(event.target.value);
    this.setState({ pageSize: pageSize });
    onChange(currentPage, pageSize);
  };

  _onChangePage = (eventKey, event) => {
    const { onChange } = this.props;
    const { pageSize } = this.state;

    event.preventDefault();
    const pageNo = Number(eventKey);
    this.setState({ currentPage: pageNo });
    onChange(pageNo, pageSize);
  };

  _pageSizeSelect = () => {
    const { pageSizes, showPageSizeSelect } = this.props;
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
    const { children, totalItems } = this.props;
    const { currentPage, pageSize } = this.state;

    const numberPages = Math.ceil(totalItems / pageSize);

    return (
      <span>
        {this._pageSizeSelect()}

        {children}

        <div className="text-center">
          <Pagination items={numberPages}
                      maxButtons={10}
                      activePage={currentPage}
                      onSelect={this._onChangePage}
                      prev
                      next
                      first
                      last />
        </div>
      </span>
    );
  }
}

export default PaginatedList;
