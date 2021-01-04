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
import Promise from 'bluebird';
import styled from 'styled-components';

import { Button } from 'components/graylog';
import Spinner from 'components/common/Spinner';

const FormContent = styled.div(({ buttonLeftMargin }) => `
  > :not(:last-child) {
    margin-right: ${buttonLeftMargin}px;
  }

  > * {
    display: inline-block;
    vertical-align: top;
    margin-bottom: 5px;
  }
`);

const HelpFeedback = styled.span`
  &.form-control-feedback {
    pointer-events: auto;
  }

  .btn {
    max-width: 34px;
  }
`;

/**
 * Component that renders a customizable search form. The component
 * supports a loading state, adding children next to the form, and
 * styles customization.
 */
class SearchForm extends React.Component {
  static propTypes = {
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
    onSearch: PropTypes.func.isRequired,
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
    /** bsStyle for search button. */
    searchBsStyle: PropTypes.string,
    /** Text to display in the search button. */
    searchButtonLabel: PropTypes.node,
    /** Text to display in the reset button. */
    resetButtonLabel: PropTypes.node,
    /**
     * Text to display in the search button while the search is loading. This
     * will only be used if `useLoadingState` is true.
     */
    loadingLabel: PropTypes.string,
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
     * instance, a `Button` component with `bsStyle="link"` and a font-awesome icon
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

  static defaultProps = {
    query: '',
    className: '',
    onQueryChange: () => {},
    onReset: null,
    label: null,
    placeholder: 'Enter search query...',
    wrapperClass: 'search',
    queryWidth: 'auto',
    topMargin: 15,
    buttonLeftMargin: 5,
    searchBsStyle: 'default',
    searchButtonLabel: 'Search',
    resetButtonLabel: 'Reset',
    useLoadingState: false,
    loadingLabel: 'Loading...',
    queryHelpComponent: null,
    children: null,
    focusAfterMount: false,
  };

  constructor(props) {
    super(props);

    this.state = {
      query: props.query,
      isLoading: false,
    };
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    const { query } = this.props;

    // The query might get reset outside of this component so we have to adjust the internal state
    if (query !== nextProps.query) {
      this.setState({ query: nextProps.query });
    }
  }

  /**
   * This sets the loading state and returns a promise which gets resolved once the loading state is set.
   * Callers of this function should only continue once the promise got resolved to avoid race conditions
   * with setting the loading state. Otherwise it can happen that the loading state gets set to "false"
   * before setting it to "true" has happened and thus not resetting the state after a search request.
   * @private
   */
  _setLoadingState = () => {
    const { useLoadingState } = this.props;

    return new Promise((resolve) => {
      if (useLoadingState) {
        this.setState({ isLoading: true }, resolve);
      } else {
        resolve();
      }
    });
  };

  _resetLoadingState = () => {
    const { useLoadingState } = this.props;

    if (useLoadingState) {
      this.setState({ isLoading: false });
    }
  };

  _onSearch = (e) => {
    const { useLoadingState, onSearch } = this.props;
    const { query } = this.state;

    e.preventDefault();
    e.stopPropagation();

    if (useLoadingState) {
      this._setLoadingState().then(() => {
        onSearch(query, this._resetLoadingState);
      });
    } else {
      onSearch(query);
    }
  };

  _onReset = () => {
    this._resetLoadingState();
    const { query, onQueryChange, onReset } = this.props;

    this.setState({ query: query });
    onQueryChange(query);
    onReset();
  };

  handleQueryChange = (e) => {
    const { onQueryChange } = this.props;
    const query = e.target.value;

    this.setState({ query: query });
    onQueryChange(query);
  };

  render() {
    const {
      queryHelpComponent,
      queryWidth,
      focusAfterMount,
      children,
      className,
      placeholder,
      resetButtonLabel,
      buttonLeftMargin,
      label,
      onReset,
      wrapperClass,
      topMargin,
      searchButtonLabel,
      loadingLabel,
      searchBsStyle,
    } = this.props;
    const { query, isLoading } = this.state;

    return (
      <div className={`${wrapperClass} ${className}`} style={{ marginTop: topMargin }}>
        <form className="form-inline" onSubmit={this._onSearch}>
          <FormContent buttonLeftMargin={buttonLeftMargin}>
            <div className={`form-group ${queryHelpComponent ? 'has-feedback' : ''}`}>
              {label && (
                <label htmlFor="common-search-form-query-input" className="control-label">
                  {label}
                </label>
              )}
              <input id="common-search-form-query-input"
                   /* eslint-disable-next-line jsx-a11y/no-autofocus */
                     autoFocus={focusAfterMount}
                     onChange={this.handleQueryChange}
                     value={query}
                     placeholder={placeholder}
                     type="text"
                     style={{ width: queryWidth }}
                     className="query form-control"
                     autoComplete="off"
                     spellCheck="false" />
              {queryHelpComponent && (
                <HelpFeedback className="form-control-feedback">{queryHelpComponent}</HelpFeedback>
              )}
            </div>

            <Button bsStyle={searchBsStyle}
                    type="submit"
                    disabled={isLoading}
                    className="submit-button">
              {isLoading ? <Spinner text={loadingLabel} delay={0} /> : searchButtonLabel}
            </Button>

            {onReset && (
              <Button type="reset" className="reset-button" onClick={this._onReset}>
                {resetButtonLabel}
              </Button>
            )}
            {children}
          </FormContent>
        </form>
      </div>
    );
  }
}

export default SearchForm;
