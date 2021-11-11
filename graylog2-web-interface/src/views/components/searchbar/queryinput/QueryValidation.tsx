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
import { debounce } from 'lodash';
import { useState, useEffect } from 'react';

import { Popover } from 'components/bootstrap';
import { OverlayTrigger, Icon } from 'components/common';
import { QueriesActions } from 'views/actions/QueriesActions';

const Container = styled.div`
  display: flex;
  align-items: center;
  padding-top: 2px;
  margin-right: 5px;
  margin-left: 5px;
`;

const ErrorIcon = styled(Icon)(({ theme, $status }: { theme: DefaultTheme, $status: string}) => `
  color: ${$status === 'ERROR' ? theme.colors.variant.danger : theme.colors.variant.warning};
  font-size: 22px;
`);

const validateQuery = debounce((value, setValidationState) => {
  QueriesActions.validateQueryString(value).then((result) => {
    setValidationState(result);
  });
}, 350);

const useValidateQuery = (value) => {
  const [validationState, setValidationState] = useState(undefined);

  useEffect(() => {
    if (value) {
      validateQuery(value, setValidationState);
    }
  }, [value]);

  useEffect(() => {
    if (!value && validationState) {
      setValidationState(undefined);
    }
  }, [value, validationState]);

  return validationState;
};

type Props = {
  query: string | undefined,
}

const QueryValidation = ({ query }: Props) => {
  const validationState = useValidateQuery(query);

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
        <ErrorIcon $status={status} name="exclamation-circle" />
      </OverlayTrigger>
    </Container>
  );
};

export default QueryValidation;
