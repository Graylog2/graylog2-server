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
import PropTypes from 'prop-types';
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
  useLoadingState,
  queryHelpComponent,
  queryWidth,
  focusAfterMount,
  children,
  className,
  placeholder,
  buttonLeftMargin,
  label,
  onReset,
  onSearch,
  wrapperClass,
  topMargin,
  onQueryChange,
  query: propsQuery,
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

SearchForm.propTypes = {
  /** The query string value. */
  query: PropTypes.string,
  /**
   * Callback that gets called on every update of the query string.
   * The first argument of the function is the query string.
   */
  onQueryChange: PropTypes.func,
  /**
   * Callback when a search was submitted. The function receives the query
   * and a callback to reset the loading state of the form as arguments.
   */
  onSearch: PropTypes.func,
  /** Callback when the input was reset. The function is called with no arguments. */
  onReset: PropTypes.func,
  /** Search field label. */
  label: PropTypes.string,
  /** The className is needed to override the component style with styled-components  */
  className: PropTypes.string,
  /** Search field placeholder. */
  placeholder: PropTypes.string,
  /** Class name for the search form container. */
  wrapperClass: PropTypes.string,
  /** Width to use in the search field. */
  queryWidth: PropTypes.any,
  /** Top margin to use in the search form container. */
  topMargin: PropTypes.number,
  /** Separation between search field and buttons. */
  buttonLeftMargin: PropTypes.number,
  /**
   * Specifies if it should display a loading state from the moment the
   * search button is pressed until the component receives new props or
   * the callback function in the `onSearch` method is called.
   */
  useLoadingState: PropTypes.bool,
  /**
   * Specifies a component that should be render inside the search input
   * field, and is meant to act as a trigger to display help about the query.
   * You may want to enlarge `queryWidth` to give the user more room to write the
   * query if you use this prop.
   *
   * **Note:** Due to size constraints rendering this component inside the input,
   * this component should contain very little text and should be very light. For
   * instance, a `Button` component with `bsStyle="link"` and an icon
   * inside would work just fine.
   */
  queryHelpComponent: PropTypes.element,
  /** Elements to display on the right of the search form. */
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]),
  focusAfterMount: PropTypes.bool,
};

SearchForm.defaultProps = {
  query: '',
  className: '',
  onQueryChange: undefined,
  onSearch: null,
  onReset: null,
  label: null,
  placeholder: 'Enter search query...',
  wrapperClass: 'search',
  queryWidth: 400,
  topMargin: 0,
  buttonLeftMargin: 5,
  useLoadingState: false,
  queryHelpComponent: null,
  children: null,
  focusAfterMount: false,
};

export default SearchForm;
