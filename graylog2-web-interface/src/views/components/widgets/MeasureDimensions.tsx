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
import { useState, useEffect, useRef, useCallback } from 'react';

type Props = {
  children: React.ReactElement,
};

const MeasureDimensions = ({ children }: Props) => {
  const container = useRef<HTMLSpanElement>(null);
  const [height, setHeight] = useState<number|undefined>();

  const _getHeight = useCallback(() => {
    if (container) {
      const { current } = container;

      return current.offsetHeight;
    }

    return undefined;
  }, []);

  const _setHeight = useCallback(() => {
    setHeight(_getHeight());
  }, [_getHeight]);

  useEffect(() => {
    window.addEventListener('resize', _setHeight);
    _setHeight();

    return () => window.removeEventListener('resize', _setHeight);
  }, [_setHeight]);

  const _renderChildren = () => {
    return React.Children.map(children, (child) => {
      return React.cloneElement(child, {
        containerHeight: height,
      });
    });
  };

  return (
    <span ref={container} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {_renderChildren()}
    </span>
  );
};

export default MeasureDimensions;
