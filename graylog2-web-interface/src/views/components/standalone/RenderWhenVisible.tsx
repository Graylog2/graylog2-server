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
import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';

// Takes the space the content will take, so the layout does not jump when it renders.
const Placeholder = styled.div`
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
`;

type Props = React.PropsWithChildren<{
  // How far outside the viewport the content already renders, so it is usually ready when it scrolls into view.
  rootMargin?: string;
}>;

/**
 * Renders its children once they come close to the viewport, and keeps them rendered afterwards.
 */
const RenderWhenVisible = ({ rootMargin = '200px', children = undefined }: Props) => {
  const placeholderRef = useRef<HTMLDivElement>(null);
  // Without IntersectionObserver, e.g. in tests, there is no way to tell, so the children render right away.
  const [isVisible, setIsVisible] = useState(typeof IntersectionObserver === 'undefined');

  useEffect(() => {
    const placeholder = placeholderRef.current;

    if (isVisible || !placeholder) {
      return undefined;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
        }
      },
      { rootMargin },
    );
    observer.observe(placeholder);

    return () => observer.disconnect();
  }, [isVisible, rootMargin]);

  return isVisible ? <>{children}</> : <Placeholder ref={placeholderRef} />;
};

export default RenderWhenVisible;
