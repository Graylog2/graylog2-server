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

import Button from 'components/bootstrap/Button';
import OverlayDropdown from 'components/common/OverlayDropdown';
import type { BsSize } from 'components/bootstrap/types';
import { Icon } from 'components/common/index';

type Props = {
  alwaysShowCaret?: boolean,
  bsSize?: BsSize,
  buttonTitle?: string,
  children: React.ReactNode | ((payload: { toggleDropdown: () => void }) => React.ReactNode),
  closeOnSelect?: boolean,
  disabled?: boolean
  dropdownZIndex?: number,
  onToggle?: (isOpen: boolean) => void,
  title: React.ReactNode,
};

/**
 * This component is an alternative to the `DropdownButton` component and displays the dropdown in a portal.
 */
const OverlayDropdownButton = ({
  alwaysShowCaret = false,
  bsSize,
  buttonTitle,
  children,
  closeOnSelect = true,
  disabled = false,
  dropdownZIndex,
  onToggle: onToggleProp,
  title,
}: Props) => {
  const [show, setShowDropdown] = useState(false);

  const _onToggle = () => {
    if (typeof onToggleProp === 'function') {
      onToggleProp(!show);
    }

    setShowDropdown((cur) => !cur);
  };

  return (
    <OverlayDropdown show={show}
                     closeOnSelect={closeOnSelect}
                     dropdownZIndex={dropdownZIndex}
                     alwaysShowCaret={alwaysShowCaret}
                     toggleChild={(
                       <div className={`dropdown btn-group ${show ? 'open' : ''}`}>
                         <Button bsSize={bsSize}
                                 className="dropdown-toggle"
                                 aria-label={buttonTitle}
                                 title={buttonTitle}
                                 disabled={disabled}>
                           {title} <Icon name="arrow_drop_down" />
                         </Button>
                       </div>
                     )}
                     placement="bottom"
                     onToggle={_onToggle}>
      {typeof children === 'function' ? children({ toggleDropdown: _onToggle }) : children}
    </OverlayDropdown>
  );
};

export default OverlayDropdownButton;
