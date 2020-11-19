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

import { ThemeInterface } from 'theme';
import { Alert, Button, Col, Row } from 'components/graylog';
import Icon from 'components/common/Icon';

const StyledRow = styled(Row)`
  margin-bottom: 0 !important;
`;

const Container: StyledComponent<{margin: number}, ThemeInterface, HTMLElement> = styled.div(({ margin }) => `
  margin-top: ${margin}px;
  margin-bottom: ${margin}px;
`);

type Props = {
  onClose: (msg: string | undefined) => void,
  children: JSX.Element | string,
  bsStyle: string,
  marginTopBottom: number,
  runtimeError: boolean,
};

const ErrorAlert = ({ children, onClose, bsStyle = 'warning', marginTopBottom = 15, runtimeError }: Props) => {
  const finalBsStyle = runtimeError ? 'danger' : bsStyle;

  if (!children) {
    return null;
  }

  return (
    <Container margin={marginTopBottom}>
      <Alert bsStyle={finalBsStyle}>
        <StyledRow>
          <Col md={11}>
            {runtimeError && <h4>Runtime Error</h4>}
            {children}
          </Col>
          <Col md={1}>
            <Button bsSize="xsmall" bsStyle={finalBsStyle} className="pull-right" onClick={() => onClose(undefined)}>
              <Icon name="times" />
            </Button>
          </Col>
        </StyledRow>
      </Alert>
    </Container>
  );
};

ErrorAlert.propTypes = {
  bsStyle: PropTypes.string,
  runtimeError: PropTypes.bool,
  marginTopBottom: PropTypes.number,
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
    PropTypes.string,
  ]),
  onClose: PropTypes.func,
};

ErrorAlert.defaultProps = {
  bsStyle: 'warning',
  runtimeError: false,
  marginTopBottom: 15,
  children: null,
  onClose: () => undefined,
};

export default ErrorAlert;
