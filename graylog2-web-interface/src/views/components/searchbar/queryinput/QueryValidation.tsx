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
import { debounce, uniq, isEmpty } from 'lodash';
import { useState, useEffect, useRef } from 'react';
import { Overlay, Transition } from 'react-overlays';
import BluebirdPromise from 'bluebird';

import { Popover } from 'components/bootstrap';
import { Icon } from 'components/common';
import { QueriesActions, QueryValidationState } from 'views/stores/QueriesStore';
import StringUtils from 'util/StringUtils';
import { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import { useStore } from 'stores/connect';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchStore } from 'views/stores/SearchStore';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

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

const DocumentationIcon = styled(Icon)`
  margin-left: 5px;
`;

const Title = styled.div`
  display: flex;
  justify-content: space-between;
`;

const ExplanationTitle = ({ title }: { title: string }) => (
  <Title>
    {title}
    <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                       title="Search query syntax documentation"
                       text={<DocumentationIcon name="lightbulb" />} />
  </Title>
);

const validateQuery = debounce(({ queryString, timeRange, streams, setValidationState, parameters, parameterBindings, filter }, validationPromise: React.MutableRefObject<BluebirdPromise>) => {
  if (validationPromise.current) {
    validationPromise.current.cancel();
  }

  // eslint-disable-next-line no-param-reassign
  validationPromise.current = QueriesActions.validateQuery({
    queryString,
    timeRange,
    streams,
    parameters,
    parameterBindings,
    filter,
  }).then((result) => {
    setValidationState(result);
  }).finally(() => {
    // eslint-disable-next-line no-param-reassign
    validationPromise.current = undefined;
  });
}, 350);

const useValidateQuery = ({ queryString, timeRange, streams, parameterBindings, parameters, filter }): QueryValidationState | undefined => {
  const validationPromise = useRef<BluebirdPromise>(undefined);
  const [validationState, setValidationState] = useState(undefined);

  useEffect(() => {
    if (queryString || filter) {
      validateQuery({ queryString, timeRange, streams, setValidationState, parameters, parameterBindings, filter }, validationPromise);
    }
  }, [filter, queryString, timeRange, streams, parameterBindings, parameters, validationPromise]);

  useEffect(() => {
    if (!queryString && !filter && validationState) {
      setValidationState(undefined);
    }
  }, [queryString, filter, validationState]);

  return validationState;
};

const getExplanationTitle = (status, explanations) => {
  const baseTitle = StringUtils.capitalizeFirstLetter(status.toLocaleLowerCase());
  const errorTitles = explanations.map(({ message: { errorType } }) => errorType);
  const uniqErrorTitles = uniq(errorTitles).join(', ');

  return `${baseTitle} (${uniqErrorTitles})`;
};

const uniqErrorMessages = (explanations) => {
  const errorMessages = explanations.map(({ message: { errorMessage } }) => errorMessage);

  return uniq(errorMessages).join('. ');
};

const useValidationPayload = (queryString, timeRange, streams, filter) => {
  const { parameterBindings } = useStore(SearchExecutionStateStore);
  const { search: { parameters } } = useStore(SearchStore);

  return ({
    timeRange: !isEmpty(timeRange) ? timeRange : undefined,
    filter,
    queryString,
    streams,
    parameters,
    parameterBindings,
  });
};

type Props = {
  filter?: string | undefined,
  queryString: string | undefined,
  streams?: Array<string>,
  timeRange: TimeRange | NoTimeRangeOverride | undefined,
}

const QueryValidation = ({ queryString, timeRange, streams, filter }: Props) => {
  const [showExplanation, setShowExplanation] = useState(false);
  const toggleShow = () => setShowExplanation((prevShow) => !prevShow);
  const containerRef = useRef(undefined);
  const explanationTriggerRef = useRef(undefined);
  const validationPayload = useValidationPayload(queryString, timeRange, streams, filter);
  const validationState = useValidateQuery(validationPayload);

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
  const errorMessages = uniqErrorMessages(explanations);

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
                 shouldUpdatePosition
                 transition={Transition}>
          <Popover id="query-validation-error-explanation"
                   title={<ExplanationTitle title={getExplanationTitle(status, explanations)} />}>
            {errorMessages}
          </Popover>
        </Overlay>
      )}
    </Container>
  );
};

QueryValidation.defaultProps = {
  filter: undefined,
  streams: undefined,
};

export default QueryValidation;
