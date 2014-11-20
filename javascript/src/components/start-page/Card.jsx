'use strict';

var React = require('react');

var Card = React.createClass({
    render: function () {
        var icon = null;
        var title = null;
        var classes = "card";

        if (this.props.type === "info") {
            classes = classes + " info";
            icon = <i className="icon icon-lightbulb pull-left"></i>;
        } else if (this.props.icon) {
            icon = <i className={"icon pull-left " + this.props.icon}></i>;
        }

        if (this.props.title) {
            title = <h1>{icon ? icon : ""} {this.props.title}</h1>;
        }

        return (
            <div className={classes}>
                {title ? title : icon}
                {this.props.children}
            </div>
        );
    }
});

module.exports = Card;
