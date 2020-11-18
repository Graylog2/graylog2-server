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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import FieldType from 'views/logic/fieldtypes/FieldType';

import CustomPropTypes from './CustomPropTypes';
import FieldActions from './actions/FieldActions';
import InteractiveContext from './contexts/InteractiveContext';

type Props = {
  children?: React.ReactNode,
  disabled?: boolean,
  name: string,
  menuContainer: HTMLElement | undefined | null,
  queryId: string,
  type: FieldType,
};

const Field = ({ children, disabled = false, menuContainer, name, queryId, type }: Props) => (
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

Field.propTypes = {
  children: PropTypes.node,
  disabled: PropTypes.bool,
  name: PropTypes.string.isRequired,
  menuContainer: PropTypes.object,
  queryId: PropTypes.string,
  type: CustomPropTypes.FieldType.isRequired,
};

Field.defaultProps = {
  children: null,
  disabled: false,
  menuContainer: document.body,
  queryId: undefined,
};

export default Field;
