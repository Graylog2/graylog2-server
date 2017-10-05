import React from 'react';
import Reflux from 'reflux';
import md5 from 'md5';

import { PluginStore } from 'graylog-web-plugin/plugin';

import SearchSidebar from 'enterprise/components/widgets/SearchSidebarWithoutBorder';

import SearchStore from 'enterprise/stores/SearchStore';
import SearchActions from 'enterprise/actions/SearchActions';

const _fieldAnalyzers = (filter) => PluginStore.exports('fieldAnalyzers')
  .filter(analyzer => filter !== undefined ? filter(analyzer) : true);

export default React.createClass({
  mixins: [Reflux.connect(SearchStore, 'search')],
  getInitialState() {
    return {
      showAllFields: false,
    };
  },

  _togglePageFields() {
    this.setState({ showAllFields: !this.state.showAllFields });
  },

  _toggleField(field) {
    SearchActions.toggleField(field);
  },

  render() {
    const data = Object.assign({}, this.props.data);
    const fields = data.fields.map((field) => {
      return {
        hash: md5(field),
        name: field,
        standard_selected: (field === 'message' || field === 'source'),
      };
    });

    const result = Object.assign(data, { all_fields: fields, fields: fields });
    const sidebarProps = {
      builtQuery: data.built_query,
      fields: fields,
      fieldAnalyzers: _fieldAnalyzers(),
      onFieldAnalyzer: () => {},
      onFieldToggled: this._toggleField,
      permissions: [],
      result: result,
      selectedFields: this.state.search.fields,
      shouldHighlight: true,
      showAllFields: this.state.showAllFields,
      showHighlightToggle: true,
      togglePageFields: this._togglePageFields,
    };
    return <SearchSidebar {...sidebarProps} />;
  },
});

