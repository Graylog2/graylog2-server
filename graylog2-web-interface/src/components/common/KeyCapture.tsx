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
import { useEffect } from 'react';
import Mousetrap from 'mousetrap';

type Props = {
  children?: React.ReactElement,
  shortcuts: { [key: string ]: () => void },
}

const _executeForEachKey = (shortcuts, callback) => Object.entries(shortcuts).forEach(([key, onKeyPress]) => callback(key, onKeyPress));

const KeyCapture = ({ children, shortcuts } : Props) => {
  useEffect(() => {
    _executeForEachKey(shortcuts, (key, onKeyPress) => Mousetrap.bind(key, onKeyPress));

    return () => {
      _executeForEachKey(shortcuts, (key) => Mousetrap.unbind(key));
    };
  }, [shortcuts]);

  if (!children) {
    return null;
  }

  return children;
};

export default KeyCapture;
