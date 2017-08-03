import PropTypes from 'prop-types';
import React from 'react';
import { Pagination } from 'react-bootstrap';
import { Input } from 'components/bootstrap';

const PaginatedList = React.createClass({
  propTypes: {
    children: PropTypes.node.isRequired,
    onChange: PropTypes.func.isRequired,
    pageSize: PropTypes.number,
    pageSizes: PropTypes.arrayOf(PropTypes.number),
    totalItems: PropTypes.number.isRequired,
    showPageSizeSelect: PropTypes.bool,
  },
  getDefaultProps() {
    const defaultPageSizes = [10, 50, 100];
    return {
      pageSizes: defaultPageSizes,
      pageSize: defaultPageSizes[0],
      showPageSizeSelect: true,
    };
  },
  getInitialState() {
    return { currentPage: 1, pageSize: this.props.pageSize };
  },
  _onChangePageSize(event) {
    event.preventDefault();
    const pageSize = Number(event.target.value);
    this.setState({ pageSize: pageSize });
    this.props.onChange(this.state.currentPage, pageSize);
  },
  _onChangePage(eventKey, event) {
    event.preventDefault();
    const pageNo = Number(eventKey);
    this.setState({ currentPage: pageNo });
    this.props.onChange(pageNo, this.state.pageSize);
  },
  _pageSizeSelect() {
    if (!this.props.showPageSizeSelect) {
      return null;
    }
    return (
      <div className="form-inline page-size" style={{ float: 'right' }}>
        <Input type="select" bsSize="small" label="Show:" value={this.state.pageSize} onChange={this._onChangePageSize}>
          {this.props.pageSizes.map(size => <option key={`option-${size}`} value={size}>{size}</option>)}
        </Input>
      </div>
    );
  },
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
  },
});

export default PaginatedList;
