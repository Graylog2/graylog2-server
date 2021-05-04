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
}

const StyledPanel = styled(Panel)`
  border: 0;
  border-radius: 0;
  box-shadow: none;
`;

const PanelHeading = styled(Panel.Heading)(({ theme }) => css`
  && {
    padding: 0;
    border-radius: 0;
    background-color: ${theme.colors.variant.lightest.default};
  }
`);

const PanelTitle = styled(Panel.Title)(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
  color: ${theme.colors.global.link};
  
  > a {
    padding: 3px 9px;
    display: block;
  }
`);

const PanelBody = styled(Panel.Body)(({ theme }) => css`
  ${StyledPanel} > ${PanelHeading} + .panel-collapse > & {
    background-color: ${theme.colors.global.contentBackground};
    border-top-color: ${theme.colors.variant.lighter.default};
    border-bottom-width: 0;
    color: ${theme.colors.variant.darkest.default};
  }
`);

const AccordionItem = ({ children, name, id, ...restProps }: Props) => {
  const eventKey = id ?? name.replace(/[^0-9a-zA-Z]/g, '-').toLowerCase();

  return (
    <StyledPanel {...restProps} id={id} eventKey={eventKey}>
      <PanelHeading>
        <PanelTitle toggle>
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

AccordionItem.propTypes = {
  name: PropTypes.node.isRequired,
  id: PropTypes.string,
  children: PropTypes.node.isRequired,
};

AccordionItem.defaultProps = {
  id: undefined,
};

export default AccordionItem;
