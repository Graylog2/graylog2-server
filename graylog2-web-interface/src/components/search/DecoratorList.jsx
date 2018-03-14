import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from 'react-bootstrap';

import { SortableList } from 'components/common';

import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

class DecoratorList extends React.Component {
  static propTypes = {
    decorators: PropTypes.arrayOf(PropTypes.object).isRequired,
    disableDragging: PropTypes.bool,
    onReorder: PropTypes.func,
  };

  _onReorderWrapper = (...args) => {
    if (this.props.onReorder) {
      this.props.onReorder(...args);
    }
  };

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
  }
}

export default DecoratorList;
