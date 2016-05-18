import React from 'react';
import Immutable from 'immutable';
import { Col, Row } from 'react-bootstrap';

import { LegacyHistogram, NoSearchResults, ResultTable, SearchSidebar } from 'components/search';

import StoreProvider from 'injection/StoreProvider';
const SearchStore = StoreProvider.getStore('Search');

import { PluginStore } from 'graylog-web-plugin/plugin';
import {} from 'components/field-analyzers'; // Make sure to load all field analyzer plugins!

const SearchResult = React.createClass({
  propTypes: {
    query: React.PropTypes.string,
    builtQuery: React.PropTypes.string,
    result: React.PropTypes.object.isRequired,
    histogram: React.PropTypes.object.isRequired,
    formattedHistogram: React.PropTypes.array,
    searchInStream: React.PropTypes.object,
    streams: React.PropTypes.instanceOf(Immutable.Map),
    inputs: React.PropTypes.instanceOf(Immutable.Map),
    nodes: React.PropTypes.instanceOf(Immutable.Map),
    permissions: React.PropTypes.array.isRequired,
    searchConfig: React.PropTypes.object.isRequired,
  },

  getDefaultProps() {
    return {
      query: '*',
      builtQuery: '',
      formattedHistogram: [],
      searchInStream: null,
      streams: Immutable.Map({}),
      inputs: Immutable.Map({}),
      nodes: Immutable.Map({}),
    };
  },

  getInitialState() {
    const initialFields = SearchStore.fields;
    return {
      selectedFields: this.sortFields(initialFields),
      sortField: SearchStore.sortField,
      sortOrder: SearchStore.sortOrder,
      showAllFields: false,
      shouldHighlight: true,
    };
  },

  onFieldToggled(fieldName) {
    const currentFields = this.state.selectedFields;
    let newFieldSet;
    if (currentFields.contains(fieldName)) {
      newFieldSet = currentFields.delete(fieldName);
    } else {
      newFieldSet = currentFields.add(fieldName);
    }
    this.updateSelectedFields(newFieldSet);
  },

  togglePageFields() {
    this.setState({ showAllFields: !this.state.showAllFields });
  },

  predefinedFieldSelection(setName) {
    if (setName === 'none') {
      this.updateSelectedFields(Immutable.Set());
    } else if (setName === 'all') {
      this.updateSelectedFields(Immutable.Set(this._fields().map(field => field.name)));
    } else if (setName === 'default') {
      this.updateSelectedFields(Immutable.Set(['message', 'source']));
    }
  },

  updateSelectedFields(fieldSelection) {
    const selectedFields = this.sortFields(fieldSelection);
    SearchStore.fields = selectedFields;
    this.setState({ selectedFields: selectedFields });
  },
  _fields() {
    return this.props.result[this.state.showAllFields ? 'all_fields' : 'fields'];
  },

  sortFields(fieldSet) {
    let newFieldSet = fieldSet;
    let sortedFields = Immutable.OrderedSet();

    if (newFieldSet.contains('source')) {
      sortedFields = sortedFields.add('source');
    }
    newFieldSet = newFieldSet.delete('source');
    const remainingFieldsSorted = newFieldSet.sort((field1, field2) => field1.toLowerCase().localeCompare(field2.toLowerCase()));
    return sortedFields.concat(remainingFieldsSorted);
  },

  addFieldAnalyzer(ref, field) {
    this.refs[ref].addField(field);
  },

  _fieldAnalyzers(filter) {
    return PluginStore.exports('fieldAnalyzers')
      .filter(analyzer => filter !== undefined ? filter(analyzer) : true);
  },

  _fieldAnalyzerComponents(filter) {
    return this._fieldAnalyzers(filter)
      .map((analyzer, idx) => {
        return React.createElement(analyzer.component, {
          key: idx,
          ref: analyzer.refId,
          permissions: this.props.permissions,
          query: SearchStore.query,
          page: SearchStore.page,
          rangeType: SearchStore.rangeType,
          rangeParams: SearchStore.rangeParams.toJS(),
          stream: this.props.searchInStream,
          resolution: this.props.histogram.interval,
          from: this.props.histogram.histogram_boundaries.from,
          to: this.props.histogram.histogram_boundaries.to,
        });
      });
  },

  _shouldRenderAboveHistogram(analyzer) {
    return analyzer.displayPriority > 0;
  },

  _shouldRenderBelowHistogram(analyzer) {
    return analyzer.displayPriority <= 0;
  },

  _toggleShouldHighlight() {
    this.setState({ shouldHighlight: !this.state.shouldHighlight });
  },

  render() {
    const anyHighlightRanges = Immutable.fromJS(this.props.result.messages).some(message => message.get('highlight_ranges') !== null);

    // short circuit if the result turned up empty
    if (this.props.result.total_results === 0) {
      return (
        <NoSearchResults builtQuery={this.props.builtQuery} histogram={this.props.histogram}
                         permissions={this.props.permissions} searchInStream={this.props.searchInStream} />
      );
    }
    return (
      <Row id="main-content-search">
        <Col ref="opa" md={3} sm={12} id="sidebar">
          <SearchSidebar result={this.props.result}
                         builtQuery={this.props.builtQuery}
                         selectedFields={this.state.selectedFields}
                         fields={this._fields()}
                         fieldAnalyzers={this._fieldAnalyzers()}
                         showAllFields={this.state.showAllFields}
                         togglePageFields={this.togglePageFields}
                         onFieldToggled={this.onFieldToggled}
                         onFieldAnalyzer={this.addFieldAnalyzer}
                         predefinedFieldSelection={this.predefinedFieldSelection}
                         showHighlightToggle={anyHighlightRanges}
                         shouldHighlight={this.state.shouldHighlight}
                         toggleShouldHighlight={this._toggleShouldHighlight}
                         currentSavedSearch={SearchStore.savedSearch}
                         searchInStream={this.props.searchInStream}
                         permissions={this.props.permissions}
          />
        </Col>
        <Col md={9} sm={12} id="main-content-sidebar">
          {this._fieldAnalyzerComponents((analyzer) => this._shouldRenderAboveHistogram(analyzer))}

          <LegacyHistogram formattedHistogram={this.props.formattedHistogram}
                           histogram={this.props.histogram}
                           permissions={this.props.permissions}
                           stream={this.props.searchInStream}/>

          {this._fieldAnalyzerComponents((analyzer) => this._shouldRenderBelowHistogram(analyzer))}

          <ResultTable messages={this.props.result.messages}
                       page={SearchStore.page}
                       selectedFields={this.state.selectedFields}
                       sortField={this.state.sortField}
                       sortOrder={this.state.sortOrder}
                       resultCount={this.props.result.total_results}
                       inputs={this.props.inputs}
                       streams={this.props.streams}
                       nodes={this.props.nodes}
                       highlight={this.state.shouldHighlight}
                       searchConfig={this.props.searchConfig}
          />

        </Col>
      </Row>);
  },
});

export default SearchResult;
