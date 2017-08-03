import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from 'react-bootstrap';

const EntityList = React.createClass({
  propTypes: {
    bsNoItemsStyle: PropTypes.oneOf(['info', 'success', 'warning']),
    noItemsText: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.element,
    ]),
    items: PropTypes.array.isRequired,
  },
  getDefaultProps() {
    return {
      bsNoItemsStyle: 'info',
      noItemsText: 'No items available',
    };
  },
  render() {
    if (this.props.items.length === 0) {
      return (
        <Alert bsStyle={this.props.bsNoItemsStyle}>
          <i className="fa fa-info-circle" />&nbsp;
          {this.props.noItemsText}
        </Alert>
      );
    }

    return (
      <ul className="entity-list">
        {this.props.items}
      </ul>
    );
  },
});

export default EntityList;
