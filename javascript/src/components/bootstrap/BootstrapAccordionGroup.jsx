'use strict';

var React = require('react');

var BootstrapAccordionGroup = React.createClass({
    render: function () {
        var name = null;
        var id = null;

        if (this.props.name) {
            name = this.props.name;
            id = name.replace(/ /g, "_").toLowerCase();
        }

        return (
            <div className="accordion-group">
                <div className="accordion-heading">
                    <a href={"#" + id} data-parent="#bundles" data-toggle="collapse" className="accordion-toggle">{name}</a>
                </div>
                <div className="accordion-body collapse" id={id}>
                    <div className="accordion-inner">
                        {this.props.children}
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = BootstrapAccordionGroup;
