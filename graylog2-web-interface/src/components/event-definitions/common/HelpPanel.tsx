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
import styled from 'styled-components';

import { Panel } from 'components/bootstrap';
import { Icon } from 'components/common';

import styles from './HelpPanel.css';

const IconHeader = styled(Icon)`
  margin-right: 9px;
`;

const ConditionalCollapse = ({ condition, wrapper, children }) => (condition ? wrapper(children) : children);

type HelpPanelProps = {
  bsStyle?: 'success' | 'warning' | 'danger' | 'info' | 'default' | 'primary';
  children?: React.ReactNode;
  className?: string;
  collapsible?: boolean;
  header?: React.ReactNode;
  title?: string;
  defaultExpanded?: boolean;
};

export const HelpPanel = ({
  bsStyle = 'info',
  children,
  className = '',
  collapsible = false,
  header,
  title = '',
  defaultExpanded = false,
}: HelpPanelProps) => {
  const defaultHeader = <h3><IconHeader name="info" />{title}</h3>;

  return (
    <Panel defaultExpanded={defaultExpanded}
           className={`${styles.helpPanel} ${className}`}
           bsStyle={bsStyle}>
      <Panel.Heading>
        <Panel.Title toggle={collapsible}>
          {header || defaultHeader}
        </Panel.Title>
      </Panel.Heading>
      <ConditionalCollapse condition={collapsible} wrapper={(wrapChild) => <Panel.Collapse>{wrapChild}</Panel.Collapse>}>
        <Panel.Body>
          {children}
        </Panel.Body>
      </ConditionalCollapse>
    </Panel>
  );
};

export default HelpPanel;
