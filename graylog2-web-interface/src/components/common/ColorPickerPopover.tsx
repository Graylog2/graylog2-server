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
import * as React from 'react';
import { cloneElement, useCallback, useState, useRef } from 'react';
import PropTypes from 'prop-types';

import ColorPicker from 'components/common/ColorPicker';
import Popover from 'components/common/Popover';

type Props = {
  id: string,
  placement?: 'top' | 'right' | 'bottom' | 'left',
  title?: string,
  triggerNode: React.ReactElement,
  color: string,
  colors?: Array<Array<string>>,
  onChange: (color: string, event: React.ChangeEvent<HTMLInputElement>, handleToggle: () => void) => void,
};

/**
 * Component that renders a `ColorPicker` component inside a react bootstrap
 * popover. This is meant to use in forms and UIs with limited space, as the color
 * picker will only appear when the user interacts with the trigger node.
 *
 * This component will pass any additional props to `ColorPicker`, but their validation
 * is left for that component. Please look at `ColorPicker`'s documentation for more
 * information.
 */
const ColorPickerPopover = ({ id, placement, title, triggerNode, onChange, ...rest }: Props) => {
  const [show, setShow] = useState(false);
  const toggleTarget = useRef();

  const handleToggle = useCallback(() => { setShow(!show); }, [show]);

  const onClose = useCallback(() => setShow(false), []);

  const handleChange = useCallback((newColor: string, event: React.ChangeEvent<HTMLInputElement>) => {
    onChange(newColor, event, handleToggle);
  }, [handleToggle, onChange]);

  return (
    <Popover id={id} opened={show} position={placement} onClose={onClose}>
      <Popover.Target>
        {cloneElement(triggerNode, {
          onClick: handleToggle,
          ref: toggleTarget,
        })}
      </Popover.Target>

      <Popover.Dropdown title={title}>
        <ColorPicker onChange={handleChange}
                     {...rest} />
      </Popover.Dropdown>
    </Popover>
  );
};

ColorPickerPopover.propTypes = {
  /** Provides an ID for this popover element. */
  id: PropTypes.string.isRequired,
  /** Indicates where the popover should appear. */
  placement: PropTypes.oneOf(['top', 'right', 'bottom', 'left']),
  /** Title to use in the popover header. */
  title: PropTypes.string,
  /** React node that will be used as trigger to show/hide the popover. */
  triggerNode: PropTypes.node.isRequired,
  /**
     * Function that will be called when the selected color changes.
     * The function receives the color in hexadecimal format as first argument,
     * the event as the second argument, and a callback function to hide the
     * overlay as third argument.
     */
  onChange: PropTypes.func.isRequired,
};

ColorPickerPopover.defaultProps = {
  placement: 'bottom',
  title: 'Pick a color',
  colors: undefined, // Use default color palette.
};

export default ColorPickerPopover;
