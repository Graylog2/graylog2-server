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
import styled, { css } from 'styled-components';
import { useState, useEffect, useRef } from 'react';
import debounce from 'lodash/debounce';

import Spinner from 'components/common/Spinner';

import IconButton from './IconButton';

export const SEARCH_DEBOUNCE_THRESHOLD = 300;

const FormContent = styled.div<{ $buttonLeftMargin: number }>(({ $buttonLeftMargin }) => css`
  > :not(:last-child) {
    margin-right: ${$buttonLeftMargin}px;
  }

  > * {
    display: inline-block;
    vertical-align: top;
    margin-bottom: 5px;
  }
`);

const InputFeedback = styled.div`
  position: absolute;
  right: 0;
  top: 0;
  display: flex;
  align-items: center;
  min-height: 34px;
  padding-right: 3px;
`;

const StyledContainer = styled.div<{ $topMargin: number }>(({ $topMargin }) => css`
  margin-top: ${$topMargin}px;
`);

const StyledInput = styled.input<{ $queryWidth: number, $feedbackContainerWidth: number }>(({ $queryWidth, $feedbackContainerWidth }) => css`
  width: ${$queryWidth}px;
  padding-right: ${$feedbackContainerWidth ?? 12}px;
`);

const Label = styled.label`
  margin-right: 5px;
`;

const InputContainer = styled.div`
  display: inline-block;
  position: relative;
`;

const handleQueryChange = debounce(({
  query,
  onSearch,
  useLoadingState,
  setLoadingState,
  resetLoadingState,
}: {
  query: string,
  onSearch: (query: string, resetLoadingState?: () => void) => void,
  useLoadingState: boolean,
  setLoadingState: () => Promise<void>,
  resetLoadingState: () => void,
}) => {
  if (useLoadingState) {
    setLoadingState().then(() => {
      onSearch(query, resetLoadingState);
    });
  } else {
    onSearch(query);
  }
}, SEARCH_DEBOUNCE_THRESHOLD);

type Props = {
  useLoadingState?: boolean,
  queryHelpComponent?: React.ReactNode,
  queryWidth?: number,
  focusAfterMount?: boolean,
  children?: React.ReactNode,
  className?: string,
  placeholder?: string,
  buttonLeftMargin?: number,
  label?: React.ReactNode,
  onReset?: () => void,
  onSearch?: (query: string, reset?: () => void) => void,
  wrapperClass?: string,
  topMargin?: number,
  onQueryChange?: (query: string) => void,
  query?: string,
}

/**
 * Component that renders a customizable search form. The component
 * supports a loading state, adding children next to the form, and
 * styles customization.
 */
const SearchForm = ({
  useLoadingState = false,
  queryHelpComponent = null,
  queryWidth = 400,
  focusAfterMount = false,
  children = null,
  className = '',
  placeholder = 'Enter search query...',
  buttonLeftMargin = 5,
  label = null,
  onReset = null,
  onSearch = null,
  wrapperClass = 'search',
  topMargin = 0,
  onQueryChange,
  query: propsQuery = '',
}: Props) => {
  const [query, setQuery] = useState(propsQuery);
  const [isLoading, setIsLoading] = useState(false);
  const inputFeedbackContainer = useRef<HTMLDivElement>(undefined);

  useEffect(() => {
    setQuery(propsQuery);
  }, [propsQuery]);

  /**
   * This sets the loading state and returns a promise which gets resolved once the loading state is set.
   * Callers of this function should only continue once the promise got resolved to avoid race conditions
   * with setting the loading state. Otherwise it can happen that the loading state gets set to "false"
   * before setting it to "true" has happened and thus not resetting the state after a search request.
   * @private
   */
  const setLoadingState = () => new Promise<void>((resolve) => {
    if (useLoadingState) {
      setIsLoading(true);
      resolve();
    } else {
      resolve();
    }
  });

  const resetLoadingState = () => {
    if (useLoadingState) {
      setIsLoading(false);
    }
  };

  const handleReset = () => {
    resetLoadingState();
    setQuery(propsQuery);

    if (typeof onQueryChange === 'function') {
      onQueryChange(propsQuery);
    }

    if (typeof onReset === 'function') {
      onReset();
    }
  };

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    e.preventDefault();
    e.stopPropagation();
    const newQuery = e.target.value;

    setQuery(newQuery);

    if (typeof onQueryChange === 'function') {
      onQueryChange(newQuery);
    }

    if (typeof onSearch === 'function') {
      handleQueryChange({
        query: newQuery,
        onSearch,
        useLoadingState,
        setLoadingState,
        resetLoadingState,
      });
    }
  };

  return (
    <StyledContainer className={`${wrapperClass} ${className}`} $topMargin={topMargin}>
      <FormContent $buttonLeftMargin={buttonLeftMargin}>
        <div className="form-group">
          {label && (
            <Label htmlFor="common-search-form-query-input" className="control-label">
              {label}
            </Label>
          )}
          <InputContainer className="input-container">
            <StyledInput id="common-search-form-query-input"
                         autoFocus={focusAfterMount}
                         onChange={onChange}
                         value={query}
                         placeholder={placeholder}
                         type="text"
                         $queryWidth={queryWidth}
                         className="query form-control"
                         autoComplete="off"
                         spellCheck="false"
                         $feedbackContainerWidth={inputFeedbackContainer.current?.scrollWidth} />
            <InputFeedback ref={inputFeedbackContainer}>
              {isLoading && <Spinner text="" />}
              {(query && typeof onReset === 'function') && <IconButton name="close" title="Reset search" onClick={handleReset} />}
              {queryHelpComponent}
            </InputFeedback>
          </InputContainer>
        </div>
        {children}
      </FormContent>
    </StyledContainer>
  );
};

export default SearchForm;
