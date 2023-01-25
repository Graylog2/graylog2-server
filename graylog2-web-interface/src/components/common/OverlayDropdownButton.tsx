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

type Props = {
  bsSize?: string
  children: React.ReactNode,
  disabled?: boolean
  dropdownZIndex?: number,
  title: string,
};

/**
 * This component is an alternative to the `DropdownButton` component and displays the dropdown in a portal.
 */
const OverlayDropdownButton = ({ children, title, bsSize, disabled, dropdownZIndex }: Props) => {
  const [show, setShowDropdown] = useState(false);

  return (
    <OverlayDropdown show={show}
                     dropdownZIndex={dropdownZIndex}
                     renderToggle={({ onToggle, toggleTarget }) => (
                       <div className={`dropdown btn-group ${show ? 'open' : ''}`}>
                         <Button bsSize={bsSize}
                                 className="dropdown-toggle"
                                 ref={toggleTarget}
                                 disabled={disabled}
                                 onClick={onToggle}>
                           {title} <span className="caret" />
                         </Button>
                       </div>
                     )}
                     placement="bottom"
                     onToggle={() => setShowDropdown((cur) => !cur)}>
      {children}
    </OverlayDropdown>
  );
};

OverlayDropdownButton.defaultProps = {
  bsSize: undefined,
  disabled: false,
  dropdownZIndex: undefined,
};

export default OverlayDropdownButton;
