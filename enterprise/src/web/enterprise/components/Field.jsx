import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { Button, OverlayTrigger, Popover } from 'react-bootstrap';

import { PluginStore } from 'graylog-web-plugin/plugin';


const Field = createReactClass({
  propTypes: {
    name: PropTypes.string.isRequired,
    interactive: PropTypes.bool,
    placement: PropTypes.oneOf(['right', 'left', 'top', 'bottom']),
  },

  getDefaultProps() {
    return {
      interactive: false,
      placement: 'right',
    };
  },

  render() {
    const name = this.props.name;

    if (this.props.interactive) {
      const actionOverlay = (
        <Popover id={`field-action-overlay-${name}`} title={`${name} actions`}>
          <span>How does this get layouted properly?!</span>
        </Popover>
      );
      PluginStore.exports('fieldActions').map((fieldAction) => {
        return fieldAction;
      });

      return (<OverlayTrigger trigger="click" rootClose animation={false} placement={this.props.placement} overlay={actionOverlay}>
        <Button bsSize="xs" bsStyle="link" style={{ color: '#16ace3' }}>{name}</Button>
      </OverlayTrigger>);
    } else {
      return (
        <span>{name}</span>
      );
    }
  },
});

export default Field;
