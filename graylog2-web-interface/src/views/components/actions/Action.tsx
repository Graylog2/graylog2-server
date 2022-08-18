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
import { useCallback, useState } from 'react';

import type { ActionHandlerArguments } from 'views/components/actions/ActionHandler';

import ActionDropdown from './ActionDropdown';

import OverlayDropdown from '../OverlayDropdown';

type Props = {
  children: React.ReactNode,
  element: React.ComponentType<{ active: boolean }>,
  handlerArgs: ActionHandlerArguments,
  menuContainer: HTMLElement | undefined | null,
  type: 'field' | 'value',
};

const Action = ({ type, handlerArgs, menuContainer, element: Element, children }: Props) => {
  const [open, setOpen] = useState(false);
  const [overflowingComponents, setOverflowingComponents] = useState({});

  const _onMenuToggle = useCallback(() => setOpen(!open), [open]);
  const overflowingComponentsValues: Array<React.ReactNode> = Object.values(overflowingComponents);
  const element = <Element active={open} />;

  return (
    <>
      <OverlayDropdown show={open}
                       toggle={element}
                       placement="right"
                       onToggle={_onMenuToggle}
                       menuContainer={menuContainer}>
        <ActionDropdown handlerArgs={handlerArgs}
                        type={type}
                        setOverflowingComponents={setOverflowingComponents}
                        onMenuToggle={_onMenuToggle}
                        overflowingComponents={overflowingComponents}>
          {children}
        </ActionDropdown>
      </OverlayDropdown>
      {overflowingComponentsValues}
    </>
  );
};

export default Action;
