import PropTypes from 'prop-types';
import React from 'react';
import { Pagination } from 'react-bootstrap';
import { Input } from 'components/bootstrap';

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
    /** Number of items per page. */
    pageSize: PropTypes.number,
    /** Array of different items per page that are allowed. */
    pageSizes: PropTypes.arrayOf(PropTypes.number),
    /** Total amount of items in all pages. */
    totalItems: PropTypes.number.isRequired,
    /** */
    showPageSizeSelect: PropTypes.bool,
  };

  static defaultProps = function() {
    const defaultPageSizes = [10, 50, 100];
    return {
      pageSizes: defaultPageSizes,
      pageSize: defaultPageSizes[0],
      showPageSizeSelect: true,
    };
  }();

  state = { currentPage: 1, pageSize: this.props.pageSize };

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
    if (numberPages === 0) {
      return <span>{this.props.children}</span>;
    }

    return (
      <span>
        {this._pageSizeSelect()}

        {this.props.children}

        <div className="text-center">
          <Pagination bsSize="small" items={numberPages} maxButtons={10}
                      activePage={this.state.currentPage}
                      onSelect={this._onChangePage}
                      prev next first last />
        </div>
      </span>
    );
  }
}

export default PaginatedList;
