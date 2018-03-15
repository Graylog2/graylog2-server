import PropTypes from 'prop-types';
import React from 'react';

import createReactClass from 'create-react-class';

import { Panel } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';
import { Input } from 'components/bootstrap';

const SidebarMessageField = createReactClass({
  displayName: 'SidebarMessageField',

  propTypes: {
    field: PropTypes.object,
    fieldAnalyzers: PropTypes.array,
    onFieldAnalyzer: PropTypes.func,
    onToggled: PropTypes.func,
    selected: PropTypes.bool,
    searchConfig: PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      showActions: false,
    };
  },

  componentDidMount() {
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!./SidebarMessageField.css'),

  _onFieldAnalyzer(refId, fieldName) {
    return (event) => {
      event.preventDefault();
      this.props.onFieldAnalyzer(refId, fieldName);
    };
  },

  _analyzerIsDisabled(field) {
    const disabledFields = this.props.searchConfig.analysis_disabled_fields;
    return disabledFields && disabledFields.indexOf(field) >= 0;
  },

  _fieldAnalyzersList() {
    let analyzersList;

    if (this._analyzerIsDisabled(this.props.field.name)) {
      analyzersList = (
        <li key="field-analyzers-disabled">
          Analysis features for this field have been disabled by the administrator.
        </li>
      );
    } else {
      analyzersList = this.props.fieldAnalyzers
        .sort((a, b) => naturalSort(a.displayName, b.displayName))
        .map((analyzer, idx) => {
          return (
            <li key={`field-analyzer-button-${idx}`}>
              <a href="#" onClick={this._onFieldAnalyzer(analyzer.refId, this.props.field.name)}>
                {analyzer.displayName}
              </a>
            </li>
          );
        });
    }

    return <Panel className="field-analyzer"><ul>{analyzersList}</ul></Panel>;
  },

  _toggleFieldAnalyzers(event) {
    event.preventDefault();
    this.setState({ showActions: !this.state.showActions });
  },

  render() {
    let toggleClassName = 'fa fa-fw open-analyze-field ';
    toggleClassName += this.state.showActions ? 'open-analyze-field-active fa-caret-down' : 'fa-caret-right';

    let fieldAnalyzers;
    if (this.state.showActions) {
      fieldAnalyzers = this._fieldAnalyzersList();
    }

    return (
      <li>
        <div className="pull-left">
          <a href="#" onClick={this._toggleFieldAnalyzers}><i className={toggleClassName} /></a>
        </div>
        <div className="field-selector">
          <Input id="field-selector-checkbox"
                 type="checkbox"
                 label={this.props.field.name}
                 checked={this.props.selected}
                 onChange={() => this.props.onToggled(this.props.field.name)} />

          {fieldAnalyzers}
        </div>
      </li>
    );
  },
});

export default SidebarMessageField;
