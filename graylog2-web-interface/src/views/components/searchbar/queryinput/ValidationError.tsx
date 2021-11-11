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
import styled, { DefaultTheme } from 'styled-components';

import { Popover } from 'components/bootstrap';
import { OverlayTrigger, Icon } from 'components/common';

const Container = styled.div`
  display: flex;
  align-items: center;
  padding-top: 3px;
  margin-right: 10px;
`;

const ErrorIcon = styled(Icon)(({ theme, $status }: { theme: DefaultTheme, $status: string}) => `
  color: ${$status === 'ERROR' ? theme.colors.variant.danger : theme.colors.variant.warning};
  font-size: 24px;
`);

type Props = {
  validationState: {
    status: 'WARNING' | 'ERROR' | 'OK',
    explanations: any
  } | undefined
}

const ValidationError = ({ validationState }: Props) => {
  if (!validationState || validationState.status === 'OK') {
    return null;
  }

  const { status, explanations } = validationState;

  return (
    <Container>
      <OverlayTrigger trigger={['click']}
                      placement="bottom"
                      overlay={(
                        <Popover id="query-validation-error-explanation" title={status === 'ERROR' ? 'Error' : 'Warning'}>
                          {JSON.stringify(explanations)}
                        </Popover>
                      )}>
        <ErrorIcon $status={status} name={status === 'ERROR' ? 'times-circle' : 'exclamation-triangle'} />
      </OverlayTrigger>
    </Container>
  );
};

export default ValidationError;
