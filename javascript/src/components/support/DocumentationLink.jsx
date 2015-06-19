'use strict';

var React = require('react');
var DocsHelper = require('../../util/DocsHelper');

var DocumentationLink = React.createClass({
    render() {
        return (
            <a href={DocsHelper.toString(this.props.page)} title={this.props.title} target="_blank">
                {this.props.text}
            </a>
        );
    }
});

module.exports = DocumentationLink;