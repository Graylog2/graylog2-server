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
import { styled } from 'styled-components';

import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

const StyledWrapper = styled.div`
  cursor: pointer;
`;

type Props = React.PropsWithChildren<{
  id: string,
}>

const ExpandedRowToggleWrapper = ({ id, children }: Props) => {
  const { toggleSection } = useExpandedSections();
  const _toggleSection = () => toggleSection(id, 'overriddenProfile');

  return (
    <StyledWrapper onClick={_toggleSection}>
      {children}
    </StyledWrapper>
  );
};

export default ExpandedRowToggleWrapper;
