import React from "react";
import Reflux from "reflux";

import Select from "components/common/Select";

import StoreProvider from "injection/StoreProvider";
const SystemStore = StoreProvider.getStore('System');

const LocaleSelect = React.createClass({
  mixins: [Reflux.connect(SystemStore, 'systemStoreState')],
  propTypes: {
    onChange: React.PropTypes.func,
  },
  getValue() {
    return this.refs.locale.getValue();
  },
  _formatLocales(locales) {
    const sortedLocales = Object.values(locales)
        .filter(locale => locale['language_tag'] !== 'und')
        .map(locale => {
          return {value: locale['language_tag'], label: locale['display_name']}
        })
        .sort(function(a, b) {
        const nameA = a.label.toUpperCase();
        const nameB = b.label.toUpperCase();
        if (nameA < nameB) {
            return -1;
        }
        if (nameA > nameB) {
            return 1;
        }

        return 0;
    });

    return [{value: 'und', label:'Default locale'}].concat(sortedLocales);
  },
  _renderOption(option) {
      return <span key={option.value} title="{option.value} [{option.value}]">{option.label} [{option.value}]</span>;
  },
  render() {
    const { locales } = this.state.systemStoreState;
    if (!locales) {
      return <Spinner />;
    }

    const formattedLocales = this._formatLocales(locales);
    return (
      <Select ref="locale" {...this.props}
              placeholder="Pick a locale"
              options={formattedLocales}
              optionRenderer={this._renderOption} />
    );
  },
});

export default LocaleSelect;
