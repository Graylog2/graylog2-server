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
import { useRef, useCallback } from 'react';
import styled from 'styled-components';

import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import type { InputSummary } from 'hooks/usePaginatedInputs';

type Props = {
  input: InputSummary;
};

const StyledSpan = styled.span`
  cursor: pointer;
`;

const TitleCell = ({ input }: Props) => {
  const spanRef = useRef();
  const { toggleSection, expandedSections } = useExpandedSections();

  const toggleTrafficSection = useCallback(() => toggleSection(input.id, 'title'), [input.id, toggleSection]);

  const throughputSectionIsOpen = expandedSections?.[input.id]?.includes('title');

  return (
    <StyledSpan
      title={`${throughputSectionIsOpen ? 'Hide' : 'Show'} configuration details`}
      ref={spanRef}
      onClick={toggleTrafficSection}>
      {input.title}
    </StyledSpan>
  );
};

export default TitleCell;
