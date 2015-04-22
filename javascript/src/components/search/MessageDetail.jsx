
'use strict';

var React = require('react');

var MessageDetail = React.createClass({
    render() {
        var messageUrl = "/messages/" + this.props.message.index + "/" + this.props.message.id;
        return (<div className="row">
            <div className="message-actions pull-right">
                <a href={messageUrl} className="btn btn-sm btn-info">Permalink</a>
                <a href="#" className="btn btn-sm btn-info">Copy ID</a>
                <a href="#" className="btn btn-sm btn-info">Test against stream</a>
                <a href="#" data-toggle="modal" className="btn btn-sm btn-info" style={{marginRight: 15}}>Show terms</a>
            </div>

            <h3><i className="fa fa-envelope"></i> <a href={messageUrl} style={{color: '#000'}}>{this.props.message.id}</a></h3>
        </div>);
    }
});

module.exports = MessageDetail;