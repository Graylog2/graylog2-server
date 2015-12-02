'use strict';

var React = require('react');

var BootstrapAccordion = React.createClass({
    render() {
        return (
            <div id="bundles" className="panel-group">
                {this.props.children}
            </div>
        );
    }
});

module.exports = BootstrapAccordion;
