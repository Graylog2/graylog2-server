import PropTypes from 'prop-types';
import React from 'react';

import { Alert } from 'components/graylog';
import { Icon, SortableList } from 'components/common';

// eslint-disable-next-line import/no-webpack-loader-syntax
import DecoratorStyles from '!style!css!./decoratorStyles.css';

class DecoratorList extends React.Component {
  static propTypes = {
    decorators: PropTypes.arrayOf(PropTypes.object).isRequired,
    disableDragging: PropTypes.bool,
    onReorder: PropTypes.func,
  };

  static defaultProps = {
    disableDragging: false,
    onReorder: () => {},
  };

  _onReorderWrapper = (...args) => {
    const { onReorder } = this.props;
    if (onReorder) {
      onReorder(...args);
    }
  };

  render() {
    const { decorators, disableDragging } = this.props;
    if (!decorators || decorators.length === 0) {
      return (
        <Alert bsStyle="info" className={DecoratorStyles.noDecoratorsAlert}>
          <Icon name="info-circle" />&nbsp;No decorators configured.
        </Alert>
      );
    }
    return (
      <SortableList items={decorators} onMoveItem={this._onReorderWrapper} disableDragging={disableDragging} />
    );
  }
}

export default DecoratorList;
