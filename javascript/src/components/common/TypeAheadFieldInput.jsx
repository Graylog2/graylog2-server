import React from 'react';
import ReactDOM from 'react-dom';
import Immutable from 'immutable';
import { Input } from 'react-bootstrap';
import $ from 'jquery';
import UniversalSearch from 'logic/search/UniversalSearch';

import jsRoutes from 'routing/jsRoutes';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

var TypeAheadFieldInput = React.createClass({
  componentDidMount() {
    if (this.refs.fieldInput) {
      const fieldInput = $(this.refs.fieldInput.getInputDOMNode());
      fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.SystemApiController.fields().url))
        .then((data) => {
          fieldInput.typeahead({
              hint: true,
              highlight: true,
              minLength: 1
            },
            {
              name: 'fields',
              displayKey: 'value',
              source: UniversalSearch.substringMatcher(data.fields, 'value', 6)
            });

          if (this.props.autoFocus) {
            fieldInput.focus();
            fieldInput.typeahead('close');
          }
        });

      const fieldFormGroup = ReactDOM.findDOMNode(this.refs.fieldInput);
      $(fieldFormGroup).on('typeahead:change typeahead:selected', (event) => {
        if (this.props.valueLink) {
          this.props.valueLink.requestChange(event.target.value);
        }
      });
    }
  },
  componentWillUnmount() {
    if (this.refs.fieldInput) {
      const fieldInput = $(this.refs.fieldInput.getInputDOMNode());
      fieldInput.typeahead('destroy');
      const fieldFormGroup = ReactDOM.findDOMNode(this.refs.fieldInput);
      $(fieldFormGroup).off('typeahead:change typeahead:selected');
    }
  },

  _getFilteredProps() {
    var props = Immutable.fromJS(this.props);

    if (props.has('valueLink')) {
      props = props.delete('valueLink');
    }

    return props.toJS();
  },

  render() {
    return <Input ref="fieldInput"
                  wrapperClassName="typeahead-wrapper"
                  defaultValue={this.props.valueLink.value}
      {...this._getFilteredProps()}/>;
  }
});

export default TypeAheadFieldInput;
