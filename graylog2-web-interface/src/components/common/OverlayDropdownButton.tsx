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
  children: React.ReactNode,
  title: string,
  bsSize?: string
  disabled?: boolean
};

/**
 * Component used to display a simple alert message for a search that returned no matching results.
 * Usage should include utilizing the `children` props to supply the user with a descriptive message.
 */
const OverlayDropdownButton = ({ children, title, bsSize, disabled }: Props) => {
  const [show, setShowDropdown] = useState(false);

  return (
    <OverlayDropdown show={show}
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
};

export default OverlayDropdownButton;
