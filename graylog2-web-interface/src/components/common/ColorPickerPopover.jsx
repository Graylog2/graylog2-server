import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { OverlayTrigger, Popover } from 'react-bootstrap';
import { ColorPicker } from 'components/common';

import style from './ColorPickerPopover.css';

/**
 * Component that renders a `ColorPicker` component inside a react bootstrap
 * popover. This is meant to use in forms and UIs with limited space, as the color
 * picker will only appear when the user interacts with the trigger node.
 *
 * This component will pass any additional props to `ColorPicker`, but their validation
 * is left for that component. Please look at `ColorPicker`'s documentation for more
 * information.
 */
const ColorPickerPopover = createReactClass({
  propTypes: {
    /** Provides an ID for this popover element. */
    id: PropTypes.string.isRequired,
    /** Indicates where the popover should appear. */
    placement: PropTypes.oneOf(['top', 'right', 'bottom', 'left']),
    /** Title to use in the popover header. */
    title: PropTypes.string,
    /** React node that will be used as trigger to show/hide the popover. */
    triggerNode: PropTypes.node.isRequired,
    /** Event that will show/hide the popover. */
    triggerAction: PropTypes.oneOf(['click', 'hover', 'focus']),
  },

  getDefaultProps() {
    return {
      placement: 'bottom',
      triggerAction: 'click',
      title: 'Pick a color',
    };
  },

  render() {
    const { id, placement, title, triggerNode, triggerAction, ...colorPickerProps } = this.props;
    const popover = (
      <Popover id={id} title={title} className={style.customPopover}>
        <ColorPicker {...colorPickerProps} />
      </Popover>
    );

    return (
      <OverlayTrigger trigger={triggerAction} placement={placement} overlay={popover} rootClose>
        {triggerNode}
      </OverlayTrigger>
    );
  },
});

export default ColorPickerPopover;
