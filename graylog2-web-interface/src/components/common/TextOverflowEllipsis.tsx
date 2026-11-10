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
import { useRef, useState, useLayoutEffect } from 'react';
import styled from 'styled-components';

import Tooltip from 'components/common/Tooltip';
import useElementDimensions from 'hooks/useElementDimensions';

const Wrapper = styled.div`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: normal;
`;

type Props = {
  className?: string;
  children: string;
  titleOverride?: string;
};

/**
 * Component that signals text overflow to users by using an ellipsis.
 * The parent component needs a concrete width.
 * Shows the full text in a tooltip on hover when it is actually truncated.
 * A `titleOverride` is always shown on hover, since it usually surfaces additional
 * information rather than just the untruncated version of the visible text.
 */
const TextOverflowEllipsis = ({ children, titleOverride = undefined, className = undefined }: Props) => {
  const ref = useRef<HTMLDivElement>(null);
  const { width } = useElementDimensions(ref);
  const [isTruncated, setIsTruncated] = useState(false);

  useLayoutEffect(() => {
    setIsTruncated(!!ref.current && ref.current.scrollWidth > ref.current.clientWidth);
  }, [children, width]);

  return (
    <Tooltip
      label={titleOverride || children}
      disabled={titleOverride === undefined && !isTruncated}
      multiline
      maw={400}>
      <Wrapper ref={ref} className={className}>
        {children}
      </Wrapper>
    </Tooltip>
  );
};

export default TextOverflowEllipsis;
