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
import { useContext, useState, useRef, useCallback, useEffect } from 'react';
import styled, { DefaultTheme, css, keyframes } from 'styled-components';
import { Overlay, Transition } from 'react-overlays';
import { delay } from 'lodash';
import { useFormikContext } from 'formik';

import { Popover } from 'components/bootstrap';
import Icon from 'components/common/Icon';
import StringUtils from 'util/StringUtils';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import QueryValidationActions from 'views/actions/QueryValidationActions';
import FormWarningsContext from 'contexts/FormWarningsContext';
import { QueryValidationState } from 'views/components/searchbar/queryvalidation/hooks/useValidateQuery';

const Container = styled.div`
  margin-right: 5px;
  margin-left: 5px;
  width: 25px;
`;

const ExplanationTrigger = styled.button<{ $clickable?: boolean }>(({ $clickable }) => `
  padding: 0;
  background: transparent;
  border: 0;
  display: flex;
  align-items: center;
  min-height: 35px;
  cursor: ${$clickable ? 'pointer' : 'default'};
`);

const ErrorIcon = styled(Icon)(({ theme, $status }: { theme: DefaultTheme, $status: string }) => `
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
  10%,
  90% {
    transform: translate3d(-1px, 0, 0);
  }

  20%,
  80% {
    transform: translate3d(2px, 0, 0);
  }

  30%,
  50%,
  70% {
    transform: translate3d(-4px, 0, 0);
  }

  40%,
  60% {
    transform: translate3d(4px, 0, 0);
  }
`;

const StyledPopover = styled(Popover)(({ $shaking }) => {
  if ($shaking) {
    return css`
      animation: ${shakeAnimation} 0.82s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
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

const useShakeIfAlreadyOpen = (showExplanation) => {
  const [shakingPopover, setShakingPopover] = useState(false);

  const shakePopover = useCallback(() => {
    if (!shakingPopover) {
      setShakingPopover(true);
      delay(() => setShakingPopover(false), 820);
    }
  }, [shakingPopover]);

  useEffect(() => {
    const unsubscribe = QueryValidationActions.displayValidationErrors.listen(() => {
      if (showExplanation) {
        shakePopover();
      }
    });

    return () => {
      unsubscribe();
    };
  }, [showExplanation, shakingPopover, shakePopover]);

  return shakingPopover;
};

const useListenToDisplayErrorsAction = (showExplanation, setShowExplanation) => {
  useEffect(() => {
    const unsubscribe = QueryValidationActions.displayValidationErrors.listen(() => {
      if (!showExplanation) {
        setShowExplanation(true);
      }
    });

    return () => {
      unsubscribe();
    };
  }, [showExplanation, setShowExplanation]);
};

const QueryValidation = () => {
  const [showExplanation, setShowExplanation] = useState(false);
  const toggleShow = () => setShowExplanation((prevShow) => !prevShow);
  const containerRef = useRef(undefined);
  const explanationTriggerRef = useRef(undefined);
  const shakingPopover = useShakeIfAlreadyOpen(showExplanation);
  const { errors: { queryString: queryStringErrors } } = useFormikContext();
  const { warnings } = useContext(FormWarningsContext);

  const validationState = (queryStringErrors ?? warnings?.queryString) as QueryValidationState;

  useListenToDisplayErrorsAction(showExplanation, setShowExplanation);

  // We need to always display the container to avoid query inout resizing problems
  if (!validationState || validationState.status === 'OK') {
    return (
      <Container ref={() => containerRef} />
    );
  }

  const { status, explanations } = validationState;

  return (
    <Container ref={() => containerRef}>
      <ExplanationTrigger title="Toggle validation error explanation"
                          ref={explanationTriggerRef}
                          onClick={toggleShow}
                          $clickable
                          tabIndex={0}
                          type="button">
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
                         title={(
                           <ExplanationTitle title={StringUtils.capitalizeFirstLetter(status.toLocaleLowerCase())} />
                         )}
                         $shaking={shakingPopover}>
            <div role="alert">
              {explanations.map(({ errorType, errorMessage }) => (
                <p key={errorMessage}>
                  <b>{errorType}</b>: {errorMessage}
                </p>
              ))}
            </div>
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
