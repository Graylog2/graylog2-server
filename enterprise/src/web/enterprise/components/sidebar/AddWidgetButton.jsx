import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import uuid from 'uuid/v4';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import WidgetActions from 'enterprise/actions/WidgetActions';

const AddWidgetButton = createReactClass({
  propTypes: {
    viewId: PropTypes.string.isRequired,
    queryId: PropTypes.string.isRequired,
  },

  getInitialState() {
    return {};
  },

  handleWidget(type, title) {
    return () => {
      console.log('CREATE', type, title);
      WidgetActions.create(this.props.viewId, this.props.queryId, {
        id: uuid(),
        title: title,
        type: type,
        config: {
          title: title,
          triggered: false,
          bgColor: '#8bc34a',
          triggeredBgColor: '#d32f2f',
          text: 'OK',
        },
      });
    };
  },

  render() {
    return (
      <div>
        <DropdownButton title="Add Widget">
          <MenuItem onSelect={this.handleWidget('ALERT_STATUS', 'Alert Status')}>Alert Status</MenuItem>
        </DropdownButton>
      </div>
    );
  },
});

export default AddWidgetButton;
