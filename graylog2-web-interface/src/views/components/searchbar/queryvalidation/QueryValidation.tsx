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
import styled, { DefaultTheme, css, keyframes } from 'styled-components';
import { useState, useRef } from 'react';
import { Overlay, Transition } from 'react-overlays';

import { Popover } from 'components/bootstrap';
import { Icon } from 'components/common';
import StringUtils from 'util/StringUtils';
import { TimeRange, NoTimeRangeOverride, ElasticsearchQueryString } from 'views/logic/queries/Query';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import useToggleOnSearchExecutionAttempt from './hooks/useToggleOnSearchExecutionAttempt';
import { useSyncFormErrors, useSyncFormWarnings } from './hooks/useSyncFormValidationState';
import useValidateQuery from './hooks/useValidateQuery';

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

const shakeAnimation = keyframes`
  10%, 90% {
    transform: translate3d(-1px, 0, 0);
  }
  
  20%, 80% {
    transform: translate3d(2px, 0, 0);
  }

  30%, 50%, 70% {
    transform: translate3d(-4px, 0, 0);
  }

  40%, 60% {
    transform: translate3d(4px, 0, 0);
  }
`;

const StyledPopover = styled(Popover)(({ $shaking }) => {
  if ($shaking) {
    return css`
      animation: ${shakeAnimation} 0.82s cubic-bezier(.36,.07,.19,.97) both;
    `;
  }

  return '';
});

const ExplanationTitle = ({ title }: { title: string }) => (
  <Title>
    {title}
    <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                       title="Search query syntax documentation"
                       text={<DocumentationIcon name="lightbulb" />} />
  </Title>
);

type Props = {
  filter?: ElasticsearchQueryString | string,
  queryString: ElasticsearchQueryString | string | undefined,
  streams?: Array<string>,
  timeRange: TimeRange | NoTimeRangeOverride | undefined,
}

const QueryValidation = ({ queryString, timeRange, streams, filter }: Props) => {
  const [showExplanation, setShowExplanation] = useState(false);
  const toggleShow = () => setShowExplanation((prevShow) => !prevShow);
  const containerRef = useRef(undefined);
  const explanationTriggerRef = useRef(undefined);
  const validationState = useValidateQuery({ queryString, timeRange, streams, filter });
  const shakingPopover = useToggleOnSearchExecutionAttempt(showExplanation, setShowExplanation);

  useSyncFormErrors(validationState);
  useSyncFormWarnings(validationState);

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
                 shouldUpdatePosition
                 transition={Transition}>
          <StyledPopover id="query-validation-error-explanation"
                         title={<ExplanationTitle title={StringUtils.capitalizeFirstLetter(status.toLocaleLowerCase())} />}
                         $shaking={shakingPopover}>
            {explanations.map(({ errorType, errorMessage }) => (
              <p>
                <b>{errorType}</b>: {errorMessage}
              </p>
            ))}
          </StyledPopover>
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
