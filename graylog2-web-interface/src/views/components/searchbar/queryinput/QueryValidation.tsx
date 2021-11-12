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
import { useState, useEffect, useRef } from 'react';
import { Overlay, Transition } from 'react-overlays';

import { Popover } from 'components/bootstrap';
import { Icon } from 'components/common';
import { QueriesActions, QueryValidationState } from 'views/stores/QueriesStore';
import StringUtils from 'util/StringUtils';

const Container = styled.div`
  margin-right: 5px;
  margin-left: 5px;
  width: 25px;
`;

const ExplanationTrigger = styled.div<{ $clickable?: boolean }>(({ $clickable }) => `
  display: flex;
  align-items: center;
  min-height: 35px;
  cursor: ${$clickable ? 'pointer' : 'default'};
`);

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
  const [showExplanation, setShowExplanation] = useState(false);
  const containerRef = useRef(null);
  const explanationTriggerRef = useRef(null);
  const toggleShow = () => setShowExplanation((prevShow) => !prevShow);
  const validationState = useValidateQuery(query);

  // We need to always display the container to avoid query inout resizing problems
  // we need to always display the overlay trigger to avoid overlay placement problems
  if (!validationState || validationState.status === 'OK') {
    return (
      <Container ref={() => containerRef}>
        <ExplanationTrigger ref={explanationTriggerRef} />
      </Container>
    );
  }

  const { status, explanations } = validationState;

  return (
    <Container ref={() => containerRef}>
      <ExplanationTrigger title="Toggle validation error explanation" ref={explanationTriggerRef} onClick={toggleShow} $clickable>
        <ErrorIcon $status={status} name="exclamation-circle" />
      </ExplanationTrigger>

      {showExplanation && (
        <Overlay show
                 container={containerRef.current}
                 containerPadding={10}
                 placement="bottom"
                 target={explanationTriggerRef.current}
                 transition={Transition}>
          <Popover id="query-validation-error-explanation" title={getExplanationTitle(status, explanations)}>
            {explanations.map(({ message: { errorMessage } }) => errorMessage).join('. ')}
          </Popover>
        </Overlay>
      )}
    </Container>
  );
};

export default QueryValidation;
