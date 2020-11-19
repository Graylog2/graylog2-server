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

import { Panel } from 'components/graylog';
import { Icon } from 'components/common';

import styles from './HelpPanel.css';

const IconHeader = styled(Icon)`
  margin-right: 9px;
`;

const ConditionalCollapse = ({ condition, wrapper, children }) => (condition ? wrapper(children) : children);

export const HelpPanel = ({ bsStyle, children, className, collapsible, header, title, defaultExpanded }) => {
  const defaultHeader = <h3><IconHeader name="info-circle" />{title}</h3>;

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

HelpPanel.propTypes = {
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary']),
  children: PropTypes.node,
  className: PropTypes.string,
  collapsible: PropTypes.bool,
  header: PropTypes.node,
  title: PropTypes.string,
  defaultExpanded: PropTypes.bool,
};

HelpPanel.defaultProps = {
  bsStyle: 'info',
  children: undefined,
  className: '',
  collapsible: false,
  header: undefined,
  title: '',
  defaultExpanded: false,
};

export default HelpPanel;
