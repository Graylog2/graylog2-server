/* global $ */

import React, {PropTypes} from 'react';
import {Input} from 'react-bootstrap';

require('!script!../../../public/javascripts/jquery-2.1.1.min.js');
require('!script!../../../public/javascripts/chosen.jquery.min.js');

const ChosenSelectInput = React.createClass({
  propTypes: {
    children: PropTypes.any,
    onChange: PropTypes.func,
    dataPlaceholder: PropTypes.string,
    value: PropTypes.string,
  },
  componentDidMount() {
    const $selectDOMNode = $(this.refs.select.getInputDOMNode());

    // We only want to apply the classes the parent component gave us, not those from react-bootstrap
    $selectDOMNode.on('chosen:ready', (event, params) => {
      params.chosen.container.addClass('input-sm');
    });

    $selectDOMNode.chosen({
      disable_search_threshold: 3,
      search_contains: true,
      inherit_select_classes: false,
      display_disabled_options: false,
      placeholder_text_single: this.props.dataPlaceholder,
    }).change((event, selection) => {
      this.selectedOption = selection.selected;
      if (typeof this.props.onChange === 'function') {
        this.props.onChange(selection.selected);
      }
    });
  },
  componentDidUpdate(prevProps) {
    if (this.props.children !== prevProps.children) {
      $(this.refs.select.getInputDOMNode()).trigger('chosen:updated');
    }
  },
  getValue() {
    return this.selectedOption;
  },
  render() {
    const value = this.props.value || 'placeholder';

    // Chosen needs the empty option to render the placeholder
    return (
      <Input ref="select" type="select" placeholder="placeholder" {...this.props} value={value}>
        <option value="placeholder"></option>
        {this.props.children}
      </Input>
    );
  },
});

export default ChosenSelectInput;
