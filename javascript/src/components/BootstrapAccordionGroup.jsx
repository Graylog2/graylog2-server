/** @jsx React.DOM */

'use strict';

var React = require('React');

var BootstrapAccordionGroup = React.createClass({
    render: function () {
        var name = null;

        if (this.props.name) {
            name = this.props.name;
        }

        return (
            <div className="accordion-group">
                <div className="accordion-heading">
                    <a href={"#" + name.toLowerCase()} data-parent="#bundles" data-toggle="collapse" className="accordion-toggle">{name}</a>
                </div>
                <div className="accordion-body collapse" id={name.toLowerCase()}>
                    <div className="accordion-inner">
                        {this.props.children}
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = BootstrapAccordionGroup;