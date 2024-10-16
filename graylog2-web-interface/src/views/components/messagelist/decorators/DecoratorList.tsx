import * as React from 'react';
import styled from 'styled-components';

import { Alert } from 'components/bootstrap';
import { SortableList } from 'components/common';

import DecoratorStyles from './decoratorStyles.css';

const AlertContainer = styled.div`
  margin-bottom: 20px;
`;

type ReorderedItems = Array<{ id: string }>;
type DecoratorSummary = {
  id: string,
  order?: number,
  title: React.ReactElement,
};
type Props = {
  decorators: Array<DecoratorSummary>,
  disableDragging?: boolean,
  onReorder: (items: ReorderedItems) => unknown,
};

class DecoratorList extends React.Component<Props> {
  static defaultProps = {
    disableDragging: false,
    onReorder: () => {
    },
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
            No decorators configured.
          </Alert>
        </AlertContainer>
      );
    }

    return (
      <SortableList items={decorators}
                    onMoveItem={this._onReorderWrapper}
                    disableDragging={disableDragging}
                    displayOverlayInPortal />
    );
  }
}

export default DecoratorList;
