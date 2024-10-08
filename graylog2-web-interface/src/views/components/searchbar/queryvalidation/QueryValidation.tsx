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
import { useContext, useState, useRef, useCallback, useEffect, useMemo } from 'react';
import styled, { css, keyframes } from 'styled-components';
import delay from 'lodash/delay';
import { useFormikContext } from 'formik';

import Popover from 'components/common/Popover';
import Icon from 'components/common/Icon';
import StringUtils from 'util/StringUtils';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import QueryValidationActions from 'views/actions/QueryValidationActions';
import FormWarningsContext from 'contexts/FormWarningsContext';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import usePluginEntities from 'hooks/usePluginEntities';

const Container = styled.div`
  margin-left: 5px;
  width: 25px;
  min-height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const ExplanationTrigger = styled.button<{ $clickable?: boolean }>(({ $clickable }) => css`
  padding: 0;
  background: transparent;
  border: 0;
  display: flex;
  align-items: center;
  cursor: ${$clickable ? 'pointer' : 'default'};
`);

export const DocumentationIcon = styled(Icon)`
  margin-left: 5px;
`;

const Title = styled.div`
  display: flex;
  justify-content: space-between;
`;

export const Explanation = styled.div`
  display: flex;
  justify-content: space-between;

  &:not(:last-child) {
    margin-bottom: 6px;
  }
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

type ExtraProps = {
  $shaking: boolean,
}
const StyledPopoverDropdown = styled(Popover.Dropdown)<ExtraProps>(({ $shaking }) => css`
  animation: ${$shaking ? css`${shakeAnimation} 0.82s cubic-bezier(0.36, 0.07, 0.19, 0.97) both` : 'none'};
`);
const ErrorIcon = styled(Icon)<{ $status: string }>(({ $status }) => css`
  color: ${({ theme }) => {
    if ($status === 'ERROR') return theme.colors.variant.danger;
    if ($status === 'INFO') return theme.colors.variant.info;

    return theme.colors.variant.warning;
  }};
  font-size: 22px;
  animation: ${$status === 'INFO' ? css`${shakeAnimation} 0.82s cubic-bezier(0.36, 0.07, 0.19, 0.97) both` : 'none'};
  animation-iteration-count: 2;
`);

const ExplanationTitle = ({ title }: { title: string }) => (
  <Title>
    {title}
  </Title>
);

const useShakeTemporarily = () => {
  const [shouldShake, setShake] = useState(false);

  const shake = useCallback(() => {
    if (!shouldShake) {
      setShake(true);
      delay(() => setShake(false), 820);
    }
  }, [shouldShake]);

  return [shouldShake, shake] as const;
};

const useTriggerIfErrorsPersist = (trigger: () => void) => {
  const [showExplanation, setShowExplanation] = useState(false);
  const toggleShow = useCallback(() => setShowExplanation((prevShow) => !prevShow), []);

  useEffect(() => {
    const unsubscribe = QueryValidationActions.displayValidationErrors.listen(() => {
      if (!showExplanation) {
        toggleShow();
      } else {
        trigger();
      }
    });

    return () => {
      unsubscribe();
    };
  }, [trigger, showExplanation, toggleShow]);

  return [showExplanation, toggleShow] as const;
};

type QueryForm = {
  queryString: QueryValidationState,
};

type Explanations = QueryValidationState['explanations'];

const deduplicateExplanations = (explanations: Explanations | undefined): Explanations => {
  if (explanations === null || explanations.length === 0) {
    return [];
  }

  const [deduplicated] = explanations.reduce(([prevExplanations, seen]: [Explanations, Array<string>], cur) => {
    if (cur.errorType === 'UNKNOWN_FIELD') {
      if (cur.relatedProperty !== undefined && !seen.includes(cur.relatedProperty)) {
        return [[...prevExplanations, cur], [...seen, cur.relatedProperty]];
      }

      return [prevExplanations, seen];
    }

    return [[...prevExplanations, cur], seen];
  }, [[], []]);

  return deduplicated;
};

const QueryValidation = () => {
  const plugableValidationExplanation = usePluginEntities('views.elements.validationErrorExplanation');
  const [shakingPopover, shake] = useShakeTemporarily();
  const [showExplanation, toggleShow] = useTriggerIfErrorsPersist(shake);

  const explanationTriggerRef = useRef(undefined);
  const { errors: { queryString: queryStringErrors } } = useFormikContext<QueryForm>();
  const { warnings } = useContext(FormWarningsContext);
  const validationState = (queryStringErrors ?? warnings?.queryString) as QueryValidationState;

  const { status, explanations = [] } = validationState ?? { explanations: [] };
  const deduplicatedExplanations = useMemo(() => [...deduplicateExplanations(explanations)], [explanations]);
  const hasExplanations = validationState && (validationState?.status !== 'OK');
  const isInfo = validationState && (validationState.status === 'INFO');

  const validationTitle = () => {
    if (!validationState) return '';

    switch (validationState.status) {
      case 'WARNING':
        return 'warning';
      case 'INFO':
        return 'information';
      default:
        return 'error explanation';
    }
  };

  return (
    <Popover opened={hasExplanations && showExplanation} position="bottom" width={500} withArrow>
      <Popover.Target>
        <Container ref={explanationTriggerRef}>
          {hasExplanations ? (
            <ExplanationTrigger title={`Toggle validation ${validationTitle()}`}
                                onClick={toggleShow}
                                $clickable
                                tabIndex={0}
                                type="button">
              <ErrorIcon $status={status} name={isInfo ? 'database' : 'error'} />
            </ExplanationTrigger>
          ) : (
            <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                               title="Search query syntax documentation"
                               text={<Icon name="lightbulb_circle" />} />
          )}
        </Container>
      </Popover.Target>
      {hasExplanations && showExplanation && (
        <StyledPopoverDropdown id="query-validation-error-explanation"
                               title={<ExplanationTitle title={StringUtils.capitalizeFirstLetter(status.toLocaleLowerCase())} />}
                               $shaking={shakingPopover}>
          <div role="alert">
            {deduplicatedExplanations.map(({ errorType, errorTitle, errorMessage, id }) => (
              <Explanation key={id}>
                <span><b>{errorTitle}</b>: {errorMessage}</span>
                {errorType && (
                  <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_ERRORS}
                                     title="Query error documentation"
                                     text={<DocumentationIcon name="lightbulb_circle" />} />
                )}
              </Explanation>
            ))}
            {plugableValidationExplanation?.map((PlugableExplanation, index) => (
              // eslint-disable-next-line react/no-array-index-key
              (<PlugableExplanation validationState={validationState} key={index} />)),
            )}
          </div>
        </StyledPopoverDropdown>
      )}
    </Popover>
  );
};

export default QueryValidation;
