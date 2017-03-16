import React from 'react';
import { Alert } from 'react-bootstrap';

import { SortableList } from 'components/common';

import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

const DecoratorList = React.createClass({
  propTypes: {
    decorators: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    disableDragging: React.PropTypes.bool,
    onReorder: React.PropTypes.func,
  },

  _onReorderWrapper(...args) {
    if (this.props.onReorder) {
      this.props.onReorder(...args);
    }
  },
  render() {
    if (!this.props.decorators || this.props.decorators.length === 0) {
      return (
        <Alert bsStyle="info" className={DecoratorStyles.noDecoratorsAlert}>
          <i className="fa fa-info-circle" />&nbsp;No decorators configured.
        </Alert>
      );
    }
    return (
      <SortableList items={this.props.decorators} onMoveItem={this._onReorderWrapper} disableDragging={this.props.disableDragging} />
    );
  },
});

export default DecoratorList;
