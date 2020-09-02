// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
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
  children: React.Node,
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
    PropTypes.node,
    PropTypes.string,
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
