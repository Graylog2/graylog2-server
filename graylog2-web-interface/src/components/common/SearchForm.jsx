import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'react-bootstrap';
import { Spinner } from 'components/common';

/**
 * Component that renders a customizable search form. The component
 * supports a loading state, adding children next to the form, and
 * styles customization.
 */
class SearchForm extends React.Component {
  static propTypes = {
    /**
     * Callback when a search was submitted. The function receives the query
     * and a callback to reset the loading state of the form as arguments.
     */
    onSearch: PropTypes.func.isRequired,
    /** Callback when the input was reset. The function is called with no arguments. */
    onReset: PropTypes.func,
    /** Search field label. */
    label: PropTypes.string,
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
    searchButtonLabel: PropTypes.string,
    /** Text to display in the reset button. */
    resetButtonLabel: PropTypes.string,
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
    /** Elements to display on the right of the search form. */
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]),
  };

  static defaultProps = {
    placeholder: 'Enter search query...',
    wrapperClass: 'search',
    queryWidth: 'auto',
    topMargin: 15,
    buttonLeftMargin: 5,
    searchBsStyle: 'default',
    searchButtonLabel: 'Search',
    resetButtonLabel: 'Reset',
    loadingLabel: 'Loading...',
  };

  state = {
    isLoading: false,
  };

  componentWillReceiveProps() {
    this._resetLoadingState();
  }

  _setLoadingState = () => {
    if (this.props.useLoadingState) {
      this.setState({ isLoading: true });
    }
  };

  _resetLoadingState = () => {
    if (this.props.useLoadingState) {
      this.setState({ isLoading: false });
    }
  };

  _onSearch = (e) => {
    e.preventDefault();

    this._setLoadingState();
    this.props.onSearch(this.refs.query.value, this._resetLoadingState);
  };

  _onReset = () => {
    this._resetLoadingState();
    this.refs.query.value = '';
    this.props.onReset();
  };

  render() {
    return (
      <div className={this.props.wrapperClass} style={{ marginTop: this.props.topMargin }}>
        <form className="form-inline" onSubmit={this._onSearch}>
          <div className="form-group" >
            {this.props.label && <label className="control-label">{this.props.label}</label>}
            <input ref="query"
                   placeholder={this.props.placeholder}
                   type="text"
                   style={{ width: this.props.queryWidth }}
                   label="Search"
                   className="query form-control"
                   autoComplete="off"
                   spellCheck="false" />
          </div>
          <div className="form-group" style={{ marginLeft: this.props.buttonLeftMargin }}>
            <Button bsStyle={this.props.searchBsStyle}
                    type="submit"
                    disabled={this.state.isLoading}
                    className="submit-button">
              {this.state.isLoading ? <Spinner text={this.props.loadingLabel} /> : this.props.searchButtonLabel}
            </Button>
          </div>
          {this.props.onReset &&
            <div className="form-group" style={{ marginLeft: this.props.buttonLeftMargin }}>
              <Button type="reset" className="reset-button" onClick={this._onReset}>
                {this.props.resetButtonLabel}
              </Button>
            </div>
          }
          {this.props.children}
        </form>
      </div>
    );
  }
}

export default SearchForm;
