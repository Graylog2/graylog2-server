/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

import { OverlayTrigger, Popover } from 'components/graylog';
import ColorPicker from 'components/common/ColorPicker';

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
    /**
     * Function that will be called when the selected color changes.
     * The function receives the color in hexadecimal format as first argument,
     * the event as the second argument, and a callback function to hide the
     * overlay as third argument.
     */
    onChange: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      placement: 'bottom',
      triggerAction: 'click',
      title: 'Pick a color',
    };
  },

  handleChange(color, event) {
    this.props.onChange(color, event, () => this.overlay.hide());
  },

  render() {
    const { id, placement, title, triggerNode, triggerAction, ...colorPickerProps } = this.props;
    const popover = (
      <Popover id={id} title={title} className={style.customPopover}>
        <ColorPicker {...colorPickerProps} onChange={this.handleChange} />
      </Popover>
    );

    return (
      <OverlayTrigger ref={(c) => { this.overlay = c; }}
                      trigger={triggerAction}
                      placement={placement}
                      overlay={popover}
                      rootClose>
        {triggerNode}
      </OverlayTrigger>
    );
  },
});

export default ColorPickerPopover;
