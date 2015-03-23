'use strict';

var React = require('react');

var BootstrapAccordionGroup = React.createClass({
    render() {
        var name = null;
        var id = null;

        if (this.props.name) {
            name = this.props.name;
            id = name.replace(/ /g, "_").toLowerCase();
        }

        return (
            <div className="panel panel-default">
                <div className="panel-heading">
                    <h4 className="panel-title">
                        <a href={"#" + id} data-parent="#bundles" data-toggle="collapse" className="collapsed">{name}</a>
                    </h4>
                </div>
                <div className="panel-collapse collapse" id={id}>
                    <div className="panel-body">
                        {this.props.children}
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = BootstrapAccordionGroup;
