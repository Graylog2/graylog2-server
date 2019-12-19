// @flow strict
import PropTypes from 'prop-types';
import * as React from 'react';
import styled from 'styled-components';

import { Alert } from 'components/graylog';
import { Icon, SortableList } from 'components/common';

// eslint-disable-next-line import/no-webpack-loader-syntax
import DecoratorStyles from '!style!css!./decoratorStyles.css';
import type { Decorator } from './Types';

const AlertContainer: React.ComponentType<{}> = styled.div`
  margin-bottom: 20px;
`;

type ReorderedItems = Array<{ id: string }>;
type Props = {
  decorators: Array<Decorator>,
  disableDragging?: boolean,
  onReorder: (ReorderedItems) => mixed,
};

class DecoratorList extends React.Component<Props> {
  static propTypes = {
    decorators: PropTypes.arrayOf(PropTypes.object).isRequired,
    disableDragging: PropTypes.bool,
    onReorder: PropTypes.func,
  };

  static defaultProps = {
    disableDragging: false,
    onReorder: () => {},
  };

  _onReorderWrapper = (orderedItems: ReorderedItems) => {
    const { onReorder } = this.props;
    onReorder(orderedItems);
  };

  render() {
    const { decorators, disableDragging } = this.props;
    if (!decorators || decorators.length === 0) {
      return (
        <AlertContainer>
          <Alert bsStyle="info" className={DecoratorStyles.noDecoratorsAlert}>
            <Icon name="info-circle" />&nbsp;No decorators configured.
          </Alert>
        </AlertContainer>
      );
    }
    return (
      <SortableList items={decorators} onMoveItem={this._onReorderWrapper} disableDragging={disableDragging} />
    );
  }
}

export default DecoratorList;
