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
import { useCallback } from 'react';
import styled from 'styled-components';

import { PanelGroup } from 'components/bootstrap/imports';

type Props = {
  activeKey?: string,
  children: React.ReactNode,
  defaultActiveKey?: string,
  id: string,
  onSelect?: (nextKey: string) => void,
}

const StyledPanelGroup = styled(PanelGroup)`
  margin-bottom: 0;
`;

const Accordion = ({ activeKey, children, id, onSelect = () => {}, ...restProps }:Props) => {
  const cleanActiveKey = activeKey?.replace(/[^0-9a-zA-Z-]/g, '-').toLowerCase();
  const _onSelect = useCallback((eventKey: any) => onSelect(eventKey), [onSelect]);

  return (
    <StyledPanelGroup {...restProps}
                      activeKey={cleanActiveKey}
                      id={id}
                      onSelect={_onSelect}
                      accordion>
      {children}
    </StyledPanelGroup>
  );
};

export default Accordion;
