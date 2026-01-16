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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import Button from 'components/bootstrap/Button';
import OverlayDropdown from 'components/common/OverlayDropdown';
import type { BsSize } from 'components/bootstrap/types';
import { Icon } from 'components/common/index';
import IconButton from 'components/common/IconButton';

const TRIGGER_VARIANT_BUTTON = 'button';
const TRIGGER_VARIANT_ICON_HORIZONTAL = 'icon_horizontal'; // for e.g. headers
const TRIGGER_VARIANT_ICON_VERTICAL = 'icon_vertical'; // for e.g. row actions

const StyledIconButton = styled(IconButton)(
  ({ theme }) => css`
    display: inline-block;
    margin-left: ${theme.spacings.xs};
    padding: 0;
    cursor: pointer;
  `,
);

type Props = {
  alwaysShowCaret?: boolean;
  bsSize?: BsSize;
  buttonTitle?: string;
  children: React.ReactNode | ((payload: { toggleDropdown: () => void }) => React.ReactNode);
  closeOnSelect?: boolean;
  disabled?: boolean;
  dropdownZIndex?: number;
  onToggle?: (isOpen: boolean) => void;
  title: React.ReactNode;
  triggerVariant?:
    | typeof TRIGGER_VARIANT_ICON_VERTICAL
    | typeof TRIGGER_VARIANT_ICON_HORIZONTAL
    | typeof TRIGGER_VARIANT_BUTTON;
};

/**
 * This component is an alternative to the `DropdownButton` component and displays the dropdown in a portal.
 * You can set the trigger variant to icon_horizontal or icon_vertical, to display an icon button instead of a button.
 */
const OverlayDropdownButton = ({
  alwaysShowCaret = false,
  bsSize = undefined,
  buttonTitle = undefined,
  children,
  closeOnSelect = true,
  disabled = false,
  dropdownZIndex = undefined,
  onToggle: onToggleProp = undefined,
  title,
  triggerVariant = TRIGGER_VARIANT_BUTTON,
}: Props) => {
  const [show, setShowDropdown] = useState(false);

  const _onToggle = () => {
    if (typeof onToggleProp === 'function') {
      onToggleProp(!show);
    }

    setShowDropdown((cur) => !cur);
  };

  const onClose = () => {
    if (typeof onToggleProp === 'function') {
      onToggleProp(!show);
    }
    setShowDropdown(false);
  };

  return (
    <OverlayDropdown
      show={show}
      onClose={onClose}
      closeOnSelect={closeOnSelect}
      dropdownZIndex={dropdownZIndex}
      alwaysShowCaret={alwaysShowCaret}
      toggleChild={
        <div className={`dropdown btn-group ${show ? 'open' : ''}`}>
          {triggerVariant === TRIGGER_VARIANT_BUTTON && (
            <Button
              bsSize={bsSize}
              className="dropdown-toggle"
              aria-label={buttonTitle}
              title={buttonTitle}
              disabled={disabled}>
              {title} <Icon name="arrow_drop_down" />
            </Button>
          )}
          {(triggerVariant === TRIGGER_VARIANT_ICON_VERTICAL || triggerVariant === TRIGGER_VARIANT_ICON_HORIZONTAL) && (
            <StyledIconButton
              name={triggerVariant === TRIGGER_VARIANT_ICON_VERTICAL ? 'more_vert' : 'more_horiz'}
              title={buttonTitle}
            />
          )}
        </div>
      }
      placement="bottom"
      onToggle={_onToggle}>
      {typeof children === 'function' ? children({ toggleDropdown: _onToggle }) : children}
    </OverlayDropdown>
  );
};

export default OverlayDropdownButton;
