import React from 'react';
import { Button } from 'react-bootstrap';
import { Spinner } from 'components/common';

const SearchForm = React.createClass({
  propTypes: {
    onSearch: React.PropTypes.func.isRequired,
    onReset: React.PropTypes.func,
    label: React.PropTypes.string,
    placeholder: React.PropTypes.string,
    wrapperClass: React.PropTypes.string,
    queryWidth: React.PropTypes.any,
    topMargin: React.PropTypes.number,
    buttonLeftMargin: React.PropTypes.number,
    searchBsStyle: React.PropTypes.string,
    searchButtonLabel: React.PropTypes.string,
    resetButtonLabel: React.PropTypes.string,
    loadingLabel: React.PropTypes.string,
    useLoadingState: React.PropTypes.bool,
    children: React.PropTypes.oneOfType([
      React.PropTypes.arrayOf(React.PropTypes.element),
      React.PropTypes.element,
    ]),
  },

  getDefaultProps() {
    return {
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
  },

  getInitialState() {
    return {
      isLoading: false,
    };
  },

  componentWillReceiveProps() {
    this._resetLoadingState();
  },

  _setLoadingState() {
    if (this.props.useLoadingState) {
      this.setState({ isLoading: true });
    }
  },

  _resetLoadingState() {
    if (this.props.useLoadingState) {
      this.setState({ isLoading: false });
    }
  },

  _onSearch(e) {
    e.preventDefault();

    this._setLoadingState();
    this.props.onSearch(this.refs.query.value, this._resetLoadingState);
  },

  _onReset() {
    this._resetLoadingState();
    this.refs.query.value = '';
    this.props.onReset();
  },

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
  },
});

export default SearchForm;
