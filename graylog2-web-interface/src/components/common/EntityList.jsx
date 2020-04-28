import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Alert } from 'components/graylog';
import Icon from './Icon';

const StyledList = styled.ul`
  padding: 0;
  margin: 0;
`;

/**
 * Component used to represent list of entities in Graylog, where each entity will have a title, description,
 * action buttons, etc. You need to use this component alongside `EntityListItem` in order to get a similar
 * look and feel among different entities.
 */
class EntityList extends React.Component {
  static propTypes = {
    /** bsStyle to use when there are no items in the list. */
    bsNoItemsStyle: PropTypes.oneOf(['info', 'success', 'warning']),
    /** Text to show when there are no items in the list. */
    noItemsText: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.element,
    ]),
    /** Array of `EntityListItem` that will be shown.  */
    items: PropTypes.array.isRequired,
  };

  static defaultProps = {
    bsNoItemsStyle: 'info',
    noItemsText: 'No items available',
  };

  render() {
    const { bsNoItemsStyle, items, noItemsText } = this.props;

    if (items.length === 0) {
      return (
        <Alert bsStyle={bsNoItemsStyle}>
          <Icon name="info-circle" />&nbsp;
          {noItemsText}
        </Alert>
      );
    }

    return (
      <StyledList>
        {items}
      </StyledList>
    );
  }
}

export default EntityList;
