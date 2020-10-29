// @flow strict
import * as React from 'react';
import styled, { StyledComponent } from 'styled-components';

import { Alert, Button, Col, Row } from 'components/graylog';
import Icon from 'components/common/Icon';

const StyledRow = styled(Row)`
  margin-bottom: 0px !important;
`;

const Container: StyledComponent<{margin: number}, HTMLElement<'div'>> = styled.div(({ margin }) => `
  margin-top: ${margin}px;
  margin-bottom: ${margin}px;
`);

type Props = {
  onClose: () => void,
  children: React.Node,
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
            <Button bsSize="xsmall" bsStyle={finalBsStyle} className="pull-right" onClick={() => onClose()}>
              <Icon name="times" />
            </Button>
          </Col>
        </StyledRow>
      </Alert>
    </Container>
  );
};

export default ErrorAlert;
