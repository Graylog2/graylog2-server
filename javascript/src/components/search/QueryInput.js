'use strict';

var React = require('react');

var $ = require('jquery');

var QueryInput = React.createClass({
    getInitialState() {
        return {
            query: this.props.query
        };
    },
    _onChange(event) {
      this.setState({
         query: event.target.value
      });
    },

    render() {
        return (<input type="text" id="universalsearch-query" name="q" value={this.state.query} onChange={this._onChange} placeholder="Type your search query here and press enter. (&quot;not found&quot; AND http) OR http_response_code:[400 TO 404]"/>);
    }
});

module.exports = QueryInput;
