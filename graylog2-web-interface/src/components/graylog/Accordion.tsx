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
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { PanelGroup } from './bootstrap-import';

type Props = {
  activeKey?: string,
  children: React.ReactNode,
  id: string,
  onSelect?: () => string,
}

const StyledPanelGroup = styled(PanelGroup)`
  margin-bottom: 0;
`;

const Accordion = ({ activeKey, children, id, onSelect, ...restProps }:Props) => {
  const cleanActiveKey = activeKey?.replace(/[^0-9a-zA-Z-]/g, '-').toLowerCase();

  return (
    <StyledPanelGroup {...restProps}
                      activeKey={cleanActiveKey}
                      id={id}
                      onSelect={onSelect}
                      accordion>
      {children}
    </StyledPanelGroup>
  );
};

Accordion.propTypes = {
  activeKey: PropTypes.string,
  children: PropTypes.node.isRequired,
  id: PropTypes.string.isRequired,
  onSelect: PropTypes.func,
};

Accordion.defaultProps = {
  activeKey: undefined,
  onSelect: () => {},
};

export default Accordion;
