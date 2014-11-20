'use strict';

var React = require('react');

var BootstrapAccordion = React.createClass({
    render: function () {
        return (
            <div id="bundles" className="accordion">
                {this.props.children}
            </div>
        );
    }
});

module.exports = BootstrapAccordion;
