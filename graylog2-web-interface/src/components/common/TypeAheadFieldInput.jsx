import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import Immutable from 'immutable';
import $ from 'jquery';
import { Input } from 'components/bootstrap';
// eslint-disable-next-line no-unused-vars
import Typeahead from 'typeahead.js'; // Need to import this to load typeahead, even if the variable is never used

import UniversalSearch from 'logic/search/UniversalSearch';

import ApiRoutes from 'routing/ApiRoutes';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

/**
 * Component that renders an input offering auto-completion for message fields.
 * Fields are loaded from the Graylog server in the background.
 */
class TypeAheadFieldInput extends React.Component {
  static propTypes = {
    /** ID of the input. */
    id: PropTypes.string.isRequired,
    /**
     * @deprecated React v15 deprecated `valueLink`s. Please use `onChange`
     * instead.
     */
    valueLink: PropTypes.object,
    /** Specifies if the input should have the input focus or not. */
    autoFocus: PropTypes.bool,
    /**
     * Function that is called when the input changes. The function receives
     * the typeahead event object for the event that triggered the change. For
     * more information on typeahead events, see:
     * https://github.com/twitter/typeahead.js/blob/master/doc/jquery_typeahead.md#custom-events
     */
    onChange: PropTypes.func,
  };

  componentDidMount() {
    if (this.refs.fieldInput) {
      const fieldInput = $(this.refs.fieldInput.getInputDOMNode());
      fetch('GET', URLUtils.qualifyUrl(ApiRoutes.SystemApiController.fields().url))
        .then(
          (data) => {
            fieldInput.typeahead(
              {
                hint: true,
                highlight: true,
                minLength: 1,
              },
              {
                name: 'fields',
                displayKey: 'value',
                source: UniversalSearch.substringMatcher(data.fields, 'value', 6),
              });

            if (this.props.autoFocus) {
              fieldInput.focus();
              fieldInput.typeahead('close');
            }
          });

      const fieldFormGroup = ReactDOM.findDOMNode(this.refs.fieldInput);
      $(fieldFormGroup).on('typeahead:change typeahead:selected', (event) => {
        if (this.props.onChange) {
          this.props.onChange(event);
        }
        if (this.props.valueLink) {
          this.props.valueLink.requestChange(event.target.value);
        }
      });
    }
  }

  componentWillUnmount() {
    if (this.refs.fieldInput) {
      const fieldInput = $(this.refs.fieldInput.getInputDOMNode());
      fieldInput.typeahead('destroy');
      const fieldFormGroup = ReactDOM.findDOMNode(this.refs.fieldInput);
      $(fieldFormGroup).off('typeahead:change typeahead:selected');
    }
  }

  _getFilteredProps = () => {
    let props = Immutable.fromJS(this.props);

    ['valueLink', 'onChange'].forEach((key) => {
      if (props.has(key)) {
        props = props.delete(key);
      }
    });

    return props.toJS();
  };

  render() {
    return (
      <Input id={this.props.id}
             ref="fieldInput"
             label={this.props.label}
             wrapperClassName="typeahead-wrapper"
             defaultValue={this.props.valueLink ? this.props.valueLink.value : null}
             {...this._getFilteredProps()} />
    );
  }
}

export default TypeAheadFieldInput;
