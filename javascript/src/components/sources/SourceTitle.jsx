'use strict';

var React = require('react');

var SourceTitle = React.createClass({
    render() {
        return (
            <h3 className="sources-title">
                {this.props.children}
                  <span style={{marginLeft: 10}}>
                      <button id={this.props.resetFilterId} className={"btn btn-info btn-xs " + this.props.className}
                              onClick={this.props.resetFilters} title="Reset filter" style={{display: "none"}}>
                          Reset
                      </button>
                  </span>
            </h3>
        );
    }
});

module.exports = SourceTitle;