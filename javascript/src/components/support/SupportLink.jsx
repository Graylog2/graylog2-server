'use strict';

var React = require('react/addons');

var SupportLink = React.createClass({
    render() {
        return (
            <p className="description-tooltips">
                <span className="fa-stack fa-lg">
                    <i className="fa fa-circle fa-stack-2x"></i>
                    <i className="fa fa-lightbulb-o fa-stack-1x fa-inverse"></i>
                </span>

                <strong>
                    {this.props.children}
                </strong>
            </p>
        );
    }
});

module.exports = SupportLink;
