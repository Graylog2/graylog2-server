import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

import { DecoratedSidebarMessageField, SidebarMessageField } from 'components/search';

const SearchResultsFields = styled.ul`
  margin-top: 3px;
  padding-left: 0;

  li {
    margin-top: 1px;
    margin-bottom: 1px;
    padding: 2px 0;
  }

  label {
    font-size: 12px;
    display: inline;
    position: relative;
    top: 3px;
    font-weight: normal !important;
  }

  .form-group {
    margin-bottom: 0;
  }

  .checkbox {
    margin: 0;
  }

  input[type=checkbox] {
    position: relative;
    right: 4px;
  }

  .dropdown .caret {
    display: none;
  }

  button.dropdown-toggle {
    padding: 0;
  }

  .fa-cog {
    position: relative;
    top: 3px;
  }
`;

class FieldAnalyzersSidebar extends React.Component {
  static propTypes = {
    fields: PropTypes.array,
    fieldAnalyzers: PropTypes.array,
    onFieldAnalyzer: PropTypes.func,
    onFieldToggled: PropTypes.func,
    maximumHeight: PropTypes.number,
    predefinedFieldSelection: PropTypes.func,
    result: PropTypes.object,
    searchConfig: PropTypes.object.isRequired,
    selectedFields: PropTypes.object,
    shouldHighlight: PropTypes.bool,
    showAllFields: PropTypes.bool,
    showHighlightToggle: PropTypes.bool,
    togglePageFields: PropTypes.func,
    toggleShouldHighlight: PropTypes.func,
  };

  state = {
    fieldFilter: '',
    maxFieldsHeight: 1000,
  };

  componentDidMount() {
    this._updateHeight();
    window.addEventListener('scroll', this._updateHeight);
  }

  componentDidUpdate(prevProps) {
    if (this.props.showAllFields !== prevProps.showAllFields || this.props.maximumHeight !== prevProps.maximumHeight) {
      this._updateHeight();
    }
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this._updateHeight);
  }

  MINIMUM_FIELDS_HEIGHT = 50;

  _updateHeight = () => {
    const fieldsContainer = ReactDOM.findDOMNode(this.fields);

    const footer = ReactDOM.findDOMNode(this.footer);
    const footerCss = window.getComputedStyle(footer);
    const footerMargin = parseFloat(footerCss.getPropertyValue('margin-top'));

    // Need to calculate this additionally, because margins are not included in the parent's height #computers
    let highlightToggleMargins = 0;
    if (this.highlightToggle) {
      const toggle = ReactDOM.findDOMNode(this.highlightToggle);
      const toggleCss = window.getComputedStyle(toggle);
      highlightToggleMargins = parseFloat(toggleCss.getPropertyValue('margin-top'))
        + parseFloat(toggleCss.getPropertyValue('margin-bottom'));
    }

    const maxHeight = this.props.maximumHeight
      - fieldsContainer.getBoundingClientRect().top
      - footerMargin
      - footer.offsetHeight
      - highlightToggleMargins;

    this.setState({ maxFieldsHeight: Math.max(maxHeight, this.MINIMUM_FIELDS_HEIGHT) });
  };

  _filterFields = (event) => {
    this.setState({ fieldFilter: event.target.value });
  };

  _showAllFields = (event) => {
    event.preventDefault();
    if (!this.props.showAllFields) {
      this.props.togglePageFields();
    }
  };

  _showPageFields = (event) => {
    event.preventDefault();
    if (this.props.showAllFields) {
      this.props.togglePageFields();
    }
  };

  _updateFieldSelection = (setName) => {
    this.props.predefinedFieldSelection(setName);
  };

  _updateFieldSelectionToDefault = () => {
    this._updateFieldSelection('default');
  };

  _updateFieldSelectionToAll = () => {
    this._updateFieldSelection('all');
  };

  _updateFieldSelectionToNone = () => {
    this._updateFieldSelection('none');
  };

  render() {
    const decorationStats = this.props.result.decoration_stats;
    const decoratedFields = decorationStats ? [].concat(decorationStats.added_fields || [], decorationStats.changed_fields || []) : [];
    const messageFields = this.props.fields
      .filter(field => field.name.indexOf(this.state.fieldFilter) !== -1)
      .sort((a, b) => a.name.localeCompare(b.name))
      .map((field) => {
        let messageField;
        if (decoratedFields.includes(field.name)) {
          messageField = (
            <DecoratedSidebarMessageField key={field.name}
                                          field={field}
                                          onToggled={this.props.onFieldToggled}
                                          selected={this.props.selectedFields.contains(field.name)} />
          );
        } else {
          messageField = (
            <SidebarMessageField key={field.name}
                                 field={field}
                                 fieldAnalyzers={this.props.fieldAnalyzers}
                                 onToggled={this.props.onFieldToggled}
                                 onFieldAnalyzer={this.props.onFieldAnalyzer}
                                 searchConfig={this.props.searchConfig}
                                 selected={this.props.selectedFields.contains(field.name)} />
          );
        }
        return messageField;
      });

    let shouldHighlightToggle;
    if (this.props.showHighlightToggle) {
      shouldHighlightToggle = (
        <Input ref={(highlightToggle) => { this.highlightToggle = highlightToggle; }}
               id="highligh-results-checkbox"
               type="checkbox"
               bsSize="small"
               checked={this.props.shouldHighlight}
               onChange={this.props.toggleShouldHighlight}
               label="Highlight results"
               wrapperClassName="result-highlight-control" />
      );
    }

    return (
      <div>
        <div className="input-group input-group-sm" style={{ marginTop: 5, marginBottom: 5 }}>
          <span className="input-group-btn">
            <button type="button"
                    className="btn btn-default"
                    onClick={this._updateFieldSelectionToDefault}>Default
            </button>
            <button type="button"
                    className="btn btn-default"
                    onClick={this._updateFieldSelectionToAll}>All
            </button>
            <button type="button"
                    className="btn btn-default"
                    onClick={this._updateFieldSelectionToNone}>None
            </button>
          </span>
          <input type="text"
                 className="form-control"
                 placeholder="Filter fields"
                 onChange={this._filterFields}
                 value={this.state.fieldFilter} />
        </div>
        <div ref={(fields) => { this.fields = fields; }} style={{ maxHeight: this.state.maxFieldsHeight, overflowY: 'scroll' }}>
          <SearchResultsFields>
            {messageFields}
          </SearchResultsFields>
        </div>
        <div ref={(footer) => { this.footer = footer; }} style={{ marginTop: 13, marginBottom: 0 }}>
          List{' '}
          <span className="message-result-fields-range"> fields of&nbsp;
            <a href="#"
               style={{ fontWeight: this.props.showAllFields ? 'normal' : 'bold' }}
               onClick={this._showPageFields}>current page
            </a> or{' '}
            <a href="#"
               style={{ fontWeight: this.props.showAllFields ? 'bold' : 'normal' }}
               onClick={this._showAllFields}>all fields
            </a>.
          </span>
          <br />
          {shouldHighlightToggle}
        </div>
      </div>
    );
  }
}

export default FieldAnalyzersSidebar;
