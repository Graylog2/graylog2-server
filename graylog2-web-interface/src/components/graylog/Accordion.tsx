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
import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import Panel from './Panel';

type Props = {
  children: React.ReactNode,
  name: string,
  id?: string,
  icon?: string,
}

const StyledPanel = styled(Panel)`
  border: 0;
  border-radius: 0;
`;

const PanelHeading = styled(Panel.Heading)`
  padding: 0;
  border-radius: 0;
`;

const PanelTitle = styled(Panel.Title)(({ $icon, theme }) => css`
  font-size: ${theme.fonts.size.small};
  color: ${theme.colors.global.link};
  
  > a {
    padding: 3px 9px;
  }
  // background: url("data:image/svg+xml;utf8,<svg width='24' height='24' viewBox='0 0 448 512'><path fill='#000' d='${$icon.icon[4]}'></path></svg>") center right no-repeat;
  // background-size: 24px 24px;
`);

const PanelBody = styled(Panel.Body)(({ theme }) => css`
  background-color: ${theme.colors.variant.lightest.default};
  border-top-color: ${theme.colors.variant.light.default} !important;
`);

const Accordion = ({ children, name, id, icon, ...restProps }: Props) => {
  const eventKey = id ?? name.replace(/[^0-9a-zA-Z]/g, '-').toLowerCase();

  return (
    <StyledPanel {...restProps} id={id} eventKey={eventKey}>
      <PanelHeading>
        <PanelTitle toggle $icon={icon}>
          {name}
        </PanelTitle>
      </PanelHeading>
      <Panel.Collapse>
        <PanelBody>
          {children}
        </PanelBody>
      </Panel.Collapse>
    </StyledPanel>
  );
};

Accordion.propTypes = {
  name: PropTypes.node.isRequired,
  id: PropTypes.string,
  icon: PropTypes.string,
  children: PropTypes.element.isRequired,
};

Accordion.defaultProps = {
  id: undefined,
  icon: undefined,
};

export default Accordion;
