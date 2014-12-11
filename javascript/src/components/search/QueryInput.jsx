'use strict';

var React = require('react');

var FieldsStore = require('../../stores/fields/FieldsStore');

var $ = require('jquery');

var QueryInput = React.createClass({
    getInitialState() {
        return {
            query: this.props.query
        };
    },
    componentDidMount() {
        FieldsStore.loadFields().done((fields) => {
            // TODO: Add all the other pseudo-field stuff here
            //fields.push("_missing_");
            $(this.refs.input.getDOMNode()).typeahead({ source: fields, items: 6 });
        });
    },
    _onChange(event) {
      this.setState({
         query: event.target.value
      });
    },

    render() {
        return (<input type="text" ref="input" id="universalsearch-query" name="q" autoComplete="off" placeholder="Type your search query here and press enter. (&quot;not found&quot; AND http) OR http_response_code:[400 TO 404]"/>);
    }
});

module.exports = QueryInput;
