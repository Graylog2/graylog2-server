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
import { QueriesActions, QueryValidationState } from 'views/stores/QueriesStore';
import StringUtils from 'util/StringUtils';

const Container = styled.div`
  margin-right: 5px;
  margin-left: 5px;
  width: 25px;
`;

const ErrorIconContainer = styled.div`
  min-height: 35px;
  display: flex;
  align-items: center;
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

const useValidateQuery = (value: string): QueryValidationState | undefined => {
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

const getExplanationTitle = (status, explanations) => {
  const baseTitle = StringUtils.capitalizeFirstLetter(status.toLocaleLowerCase());
  const errorTitles = explanations.map(({ message: { errorType } }) => errorType).join(', ');

  return `${baseTitle} (${errorTitles})`;
};

type Props = {
  query: string,
}

const QueryValidation = ({ query }: Props) => {
  const validationState = useValidateQuery(query);

  if (!validationState || validationState.status === 'OK') {
    return <Container />;
  }

  const { status, explanations } = validationState;

  return (
    <Container>
      <OverlayTrigger trigger={['click']}
                      placement="bottom"
                      overlay={(
                        <Popover id="query-validation-error-explanation" title={getExplanationTitle(status, explanations)}>
                          {explanations.map(({ message: { errorMessage } }) => errorMessage).join('. ')}
                        </Popover>
                      )}>
        <ErrorIconContainer title="Toggle validation error explanation">
          <ErrorIcon $status={status} name="exclamation-circle" />
        </ErrorIconContainer>
      </OverlayTrigger>
    </Container>
  );
};

export default QueryValidation;
