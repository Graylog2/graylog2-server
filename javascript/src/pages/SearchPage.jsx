import React from 'react';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';
import Immutable from 'immutable';

import InputsStore from 'stores/inputs/InputsStore';
import MessageFieldsStore from 'stores/messages/MessageFieldsStore';
import NodesStore from 'stores/nodes/NodesStore';
import StreamsStore from 'stores/streams/StreamsStore';
import UniversalSearchStore from 'stores/search/UniversalSearchStore';

import SearchStore from 'stores/search/SearchStore';

import NodesActions from 'actions/nodes/NodesActions';

import { Spinner } from 'components/common';
import { ResultTable, SearchSidebar } from 'components/search';

const SearchPage = React.createClass({
  getInitialState() {
    return {
      selectedFields: ['message', 'source'],
    };
  },
  mixins: [Reflux.connect(NodesStore), Reflux.connect(MessageFieldsStore)],
  componentDidMount() {
    const query = SearchStore.query.length > 0 ? SearchStore.query : '*';
    UniversalSearchStore.search(SearchStore.rangeType, query, SearchStore.rangeParams.toJS()).then((response) => {
      this.setState({searchResult: response});
    });
    InputsStore.list((inputs) => {
      const inputsMap = {};
      inputs.forEach((input) => inputsMap[input.input_id] = input);
      this.setState({inputs: Immutable.Map(inputsMap)});
    });

    StreamsStore.listStreams().then((streams) => {
      const streamsMap = {};
      streams.forEach((stream) => streamsMap[stream.id] = stream);
      this.setState({streams: Immutable.Map(streamsMap)});
    });

    NodesActions.list();
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

  _onToggled(fieldName) {
    if (this.state.selectedFields.indexOf(fieldName) > 0) {
      this.setState({selectedFields: this.state.selectedFields.filter((field) => field !== fieldName)});
    } else {
      this.setState({selectedFields: this.state.selectedFields.concat(fieldName)});
    }
  },

  render() {
    if (!this.state.searchResult || !this.state.inputs || !this.state.streams || !this.state.nodes || !this.state.fields) {
      return <Spinner />;
    }
    const searchResult = this.state.searchResult;
    searchResult.all_fields = this.state.fields;
    const selectedFields = this.sortFields(Immutable.List(this.state.selectedFields));
    return (
      <div id="main-content-search" className="row">
        <div ref="opa" className="col-md-3 col-sm-12" id="sidebar">
          <div ref="oma" id="sidebar-affix">
            <SearchSidebar builtQuery={searchResult.built_query} fields={searchResult.all_fields} selectedFields={selectedFields}
                           result={searchResult} permissions={['*']} onFieldToggled={this._onToggled}/>

          </div>
        </div>
        <div className="col-md-9 col-sm-12" id="main-content-sidebar">
          <ResultTable messages={this.state.searchResult.messages}
                       page={1}
                       selectedFields={selectedFields}
                       sortField={'source'}
                       sortOrder={'asc'}
                       resultCount={this.state.searchResult.total_results}
                       inputs={this.state.inputs}
                       streams={this.state.streams}
                       nodes={Immutable.Map(this.state.nodes)}
                       highlight={false} />
        </div>
      </div>
    );
  },
});

export default SearchPage;
