import React from 'react';
import Immutable from 'immutable';
import { Col, ListGroup, ListGroupItem, Row } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import { TypeAheadDataFilter } from 'components/common';

const TableList = React.createClass({
  propTypes: {
    idKey: React.PropTypes.string,
    titleKey: React.PropTypes.string,
    descriptionKey: React.PropTypes.string,
    filterKeys: React.PropTypes.arrayOf(React.PropTypes.string),
    filterLabel: React.PropTypes.string,
    items: React.PropTypes.instanceOf(Immutable.List),
    headerActionsFactory: React.PropTypes.func,
    itemActionsFactory: React.PropTypes.func,
  },
  getDefaultProps() {
    return {
      idKey: 'id',
      titleKey: 'title',
      descriptionKey: 'description',
      headerActionsFactory: () => {},
      itemActionsFactory: () => {},
    };
  },
  getInitialState() {
    return {
      filteredItems: Immutable.List(this.props.items),
      allSelected: false,
      selected: Immutable.Set(),
    };
  },
  _filterItems(filteredItems) {
    this.setState({ filteredItems: Immutable.List(filteredItems), allSelected: false });
  },
  _headerItem() {
    let bulkHeaderActions;

    if (this.state.selected.count() > 1) {
      bulkHeaderActions = this.props.headerActionsFactory(this.state.selected);
    }

    const header = (
      <div>
        {bulkHeaderActions}
        <Input type="checkbox" label="Select all" checked={this.state.allSelected} onChange={this._toggleSelectAll}
               groupClassName="form-group-inline" />
      </div>
    );
    return <ListGroupItem className="list-group-header" header={header} />;
  },
  _toggleSelectAll(event) {
    const newSelected = event.target.checked ? Immutable.Set(this.state.filteredItems.map(item => item[this.props.idKey])) : Immutable.Set();
    this.setState({ selected: newSelected, allSelected: !this.state.allSelected });
  },
  _formatItem(item) {
    const header = (
      <div>
        <div className="pull-right" style={{ marginTop: 10, marginBottom: 10 }}>
          {this.props.itemActionsFactory(item)}
        </div>

        <Input type="checkbox"
               label={item[this.props.titleKey]}
               checked={this.state.selected.includes(item[this.props.idKey])}
               onChange={this._onItemSelect(item[this.props.idKey])}
               groupClassName="form-group-inline" />
      </div>
    );
    return (
      <ListGroupItem key={`item-${item[this.props.idKey]}`} header={header}>
        <span style={{ marginLeft: 20 }}>{item[this.props.descriptionKey]}</span>
      </ListGroupItem>
    );
  },
  _onItemSelect(id) {
    return (event) => {
      const newSelected = event.target.checked ? this.state.selected.add(id) : this.state.selected.delete(id);
      this.setState({ selected: newSelected });
    };
  },
  render() {
    if (this.props.items.count() === 0) {
      return (
        <Row>
          <Col md={12}>
            <div>No items to display.</div>
          </Col>
        </Row>
      );
    }

    const formattedItems = this.state.filteredItems.map(item => this._formatItem(item)).toJS();
    let filter;

    if (this.props.filterKeys.length !== 0) {
      filter = (
        <Row>
          <Col md={5}>
            <TypeAheadDataFilter label={this.props.filterLabel}
                                 data={this.props.items}
                                 displayKey="value"
                                 filterSuggestions={[]}
                                 searchInKeys={this.props.filterKeys}
                                 onDataFiltered={this._filterItems} />
          </Col>
        </Row>
      );
    }

    if (this.state.filteredItems.count() === 0) {
      return (
        <div>
          {filter}
          <div>No items match your filter criteria</div>
        </div>
      );
    }

    return (
      <div>
        {filter}
        <ListGroup>
          {this._headerItem()}
          {formattedItems}
        </ListGroup>
      </div>
    );
  },
});

export default TableList;
