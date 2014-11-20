'use strict';

var React = require('react');

var BootstrapAccordion = React.createClass({
    render() {
        return (
            <div id="bundles" className="accordion">
                {this.props.children}
            </div>
        );
    }
});

module.exports = BootstrapAccordion;
