import React from 'react';
import { Input, Pagination } from 'react-bootstrap';

const PaginatedList = React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired,
    onChange: React.PropTypes.func.isRequired,
    pageSizes: React.PropTypes.arrayOf(React.PropTypes.number),
    totalItems: React.PropTypes.number.isRequired,
  },
  getDefaultProps() {
    return {
      pageSizes: [10, 50, 100],
    };
  },
  getInitialState() {
    return {pageSize: this.props.pageSizes[0], currentPage: 1};
  },
  _onChangePageSize(event) {
    event.preventDefault();
    const pageSize = Number(event.target.value);
    this.setState({pageSize: pageSize});
    this.props.onChange(this.state.currentPage, pageSize);
  },
  _onChangePage(event, selectedEvent) {
    event.preventDefault();
    const pageNo = Number(selectedEvent.eventKey);
    this.setState({currentPage: pageNo});
    this.props.onChange(pageNo, this.state.pageSize);
  },
  _pageSizeSelect() {
    return (
      <div className="form-inline" style={{float: 'right'}}>
        <Input type="select" bsSize="small" label="Show:" value={this.state.pageSize} onChange={this._onChangePageSize}>
          {this.props.pageSizes.map((size) => <option key={'option-' + size} value={size}>{size}</option>)}
        </Input>
      </div>
    );
  },
  render() {
    const numberPages = Math.ceil(this.props.totalItems / this.state.pageSize);
    return (
      <span>
        {this._pageSizeSelect()}

        {this.props.children}

        <div className="text-center">
          <Pagination bsSize="small" items={numberPages} maxButtons={10}
                      activePage={this.state.currentPage}
                      onSelect={this._onChangePage}
                      prev next first last/>
        </div>
      </span>
    );
  },
});

export default PaginatedList;
