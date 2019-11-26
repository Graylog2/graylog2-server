import PropTypes from 'prop-types';
import React from 'react';
import { Pagination } from 'components/graylog';
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
    currentPage: this.props.activePage > 0 ? this.props.activePage : 1,
    pageSize: this.props.pageSize,
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

  _onChangePageSize = (event) => {
    event.preventDefault();
    const pageSize = Number(event.target.value);
    this.setState({ pageSize: pageSize });
    this.props.onChange(this.state.currentPage, pageSize);
  };

  _onChangePage = (eventKey, event) => {
    event.preventDefault();
    const pageNo = Number(eventKey);
    this.setState({ currentPage: pageNo });
    this.props.onChange(pageNo, this.state.pageSize);
  };

  _pageSizeSelect = () => {
    if (!this.props.showPageSizeSelect) {
      return null;
    }
    return (
      <div className="form-inline page-size" style={{ float: 'right' }}>
        <Input id="page-size" type="select" bsSize="small" label="Show:" value={this.state.pageSize} onChange={this._onChangePageSize}>
          {this.props.pageSizes.map(size => <option key={`option-${size}`} value={size}>{size}</option>)}
        </Input>
      </div>
    );
  };

  render() {
    const numberPages = Math.ceil(this.props.totalItems / this.state.pageSize);

    return (
      <>
        {this._pageSizeSelect()}

        {this.props.children}

        <div className="text-center">
          <Pagination bsSize="small"
                      items={numberPages}
                      maxButtons={10}
                      activePage={this.state.currentPage}
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
