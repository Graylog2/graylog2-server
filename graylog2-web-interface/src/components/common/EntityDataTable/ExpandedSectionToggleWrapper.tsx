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
import React from 'react';
import { styled, css } from 'styled-components';

import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

const StyledWrapper = styled.div<{ $align?: 'right' }>(
  ({ $align }) => css`
    display: flex;
    cursor: pointer;
    height: 100%;
    width: 100%;
    align-items: center;
    justify-content: ${$align === 'right' ? 'flex-end' : 'flex-start'};
  `,
);

type Props = React.PropsWithChildren<{
  id: string;
  align?: 'right';
  section: string;
}>;

const ExpandedSectionToggleWrapper = ({ id, section, align = undefined, children = undefined }: Props) => {
  const { toggleSection } = useExpandedSections();
  const _toggleSection = () => toggleSection(id, section);

  return (
    <StyledWrapper title="Show details" onClick={_toggleSection} $align={align}>
      {children}
    </StyledWrapper>
  );
};

export default ExpandedSectionToggleWrapper;
