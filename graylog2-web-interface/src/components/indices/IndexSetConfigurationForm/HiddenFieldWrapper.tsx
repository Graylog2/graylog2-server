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

type HiddenFieldWrapperProps = {
  hiddenFields: string[];
  namePrefix?: string | null;
  ignoreFieldRestrictions: boolean;
  children: React.ReactNode;
};

const HiddenFieldWrapper = ({
  children,
  hiddenFields,
  namePrefix = null,
  ignoreFieldRestrictions,
  ...rest
}: HiddenFieldWrapperProps) => (
  <>
    {React.Children.map(children, (child) => {
      if (React.isValidElement(child)) {
        if (
          hiddenFields?.includes(namePrefix ? namePrefix + child.props.name : child.props.name) &&
          !ignoreFieldRestrictions
        )
          return null;

        return React.cloneElement(child, rest);
      }

      return child;
    })}
  </>
);

export default HiddenFieldWrapper;
