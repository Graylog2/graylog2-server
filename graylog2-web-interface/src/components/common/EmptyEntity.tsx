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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

const Container: StyledComponent<undefined, ThemeInterface, HTMLDivElement> = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;

  p {
    text-align: center;
  }
`;

const Headline = styled.h2`
  margin-top: 5px;
  margin-bottom: 10px;
  text-align: center;
`;

type Props = {
  children: JSX.Element | string,
  title: string,
};

/**
 * Component used to represent an empty entity in Graylog. This component allows us to display some larger
 * text to the user explaining what that entity is and a link to create a new one.
 */
const EmptyEntity = ({ children, title }: Props) => (
  <Container>
    <Headline>{title}</Headline>
    {children}
  </Container>
);

EmptyEntity.propTypes = {
  /** Text or node to be rendered as title. */
  title: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
  ]),
  /**
   * Any other content the component should display below the title. This may include a description and button
   * or link to easily create a new entity.
   */
  children: PropTypes.node.isRequired,
};

EmptyEntity.defaultProps = {
  title: 'Looks like there is nothing here, yet!',
};

export default EmptyEntity;
