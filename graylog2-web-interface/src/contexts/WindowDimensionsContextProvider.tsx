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
import { useEffect, useState } from 'react';
import { debounce } from 'lodash';

import WindowDimensionsContext from './WindowDimensionsContext';

type Props = {
  children: React.ReactNode,
};

const getWindowDimension = () => {
  const { innerWidth: width, innerHeight: height } = window;

  return { width, height };
};

const useWindowDimension = () => {
  const [windowDimensions, setWindowDimensions] = useState(getWindowDimension());

  useEffect(() => {
    const handleWindowResize = debounce(() => setWindowDimensions(getWindowDimension()), 200);
    window.addEventListener('resize', handleWindowResize);

    return () => {
      window.removeEventListener('resize', handleWindowResize);
    };
  });

  return windowDimensions;
};

const ExampleComponent = ({ children }: Props) => {
  const windowDimensions = useWindowDimension();

  return (
    <WindowDimensionsContext.Provider value={windowDimensions}>
      {children}
    </WindowDimensionsContext.Provider>
  );
};

export default ExampleComponent;
