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

import type FieldType from 'views/logic/fieldtypes/FieldType';

import FieldActions from './actions/FieldActions';
import InteractiveContext from './contexts/InteractiveContext';

type Props = {
  children?: React.ReactNode,
  disabled?: boolean,
  name: string,
  menuContainer?: HTMLElement,
  queryId?: string
  type: FieldType,
};

const Field = ({ children = null, disabled = false, menuContainer = document.body, name, queryId, type }: Props) => (
  <InteractiveContext.Consumer>
    {(interactive) => (interactive
      ? (
        <FieldActions element={children || name}
                      disabled={!interactive || disabled}
                      menuContainer={menuContainer}
                      name={name}
                      type={type}
                      queryId={queryId}>
          {name} = {type.type}
        </FieldActions>
      )
      : <span>{children}</span>)}
  </InteractiveContext.Consumer>
);

export default Field;
