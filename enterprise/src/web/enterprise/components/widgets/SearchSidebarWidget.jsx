import React from 'react';
import Immutable from 'immutable';
import md5 from 'md5';

import { PluginStore } from 'graylog-web-plugin/plugin';

import SearchSidebar from 'enterprise/components/widgets/SearchSidebarWithoutBorder';

const _fieldAnalyzers = (filter) => PluginStore.exports('fieldAnalyzers')
  .filter(analyzer => filter !== undefined ? filter(analyzer) : true);

const togglePageFields = () => {
  this.setState({ showAllFields: !this.state.showAllFields });
};

export default function SearchSidebarWidget(props) {
  const data = Object.assign({}, props.data);
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
    onFieldToggled: () => {},
    permissions: [],
    result: result,
    selectedFields: new Immutable.List(['source', 'message']),
    shouldHighlight: true,
    showAllFields: false,
    showHighlightToggle: true,
    togglePageFields: () => {},
  };
  return <SearchSidebar {...sidebarProps} />;
}
