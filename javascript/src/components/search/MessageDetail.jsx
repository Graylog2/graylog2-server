
'use strict';

var React = require('react');
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var Button = require('react-bootstrap').Button;
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;

var Immutable = require('immutable');

var MessageDetail = React.createClass({
    getInitialState() {
        return {
            scrollWarn: false
        };
    },
    componentDidMount() {
        var elem = React.findDOMNode(this.refs.messageList);
        if (elem && elem.clientHeight < elem.scrollHeight) {
            this.setState({scrollWarn: true});
        }
    },
    render() {
        var messageUrl = "/messages/" + this.props.message.index + "/" + this.props.message.id;

        var fields = [];
        var formattedFields = Immutable.Map(this.props.message['formatted_fields']);
        formattedFields.forEach((value, key) => {
            fields.push(<dt key={key + "dt"}>{key}</dt>);
            fields.push(
                <dd key={key + "dd"}>
                    <div className="message-field-actions pull-right">
                        <a href="#" className="btn btn-xs btn-default search-link" data-field="@f.getName" data-value="@Tools.removeTrailingNewline(r.getFormattedFields.get(f.getName))" title="Append to search query"><i className="fa fa-search-plus"></i></a>
                        <a href="#" className="btn btn-xs btn-default" title="Split into fields / Create extractor"><i className="fa fa-magic"></i></a>
                    </div>
                    {value}
                </dd>
            );
        });

        return (<div>

            <Row>
                <Col md={12}>
                    <ButtonGroup className="pull-right">
                        <Button href={messageUrl} className="btn btn-sm">Permalink</Button>
                        <Button href="#" className="btn btn-sm">Copy ID</Button>
                        <Button href="#" className="btn btn-sm">Test against stream</Button>
                        <Button href="#" data-toggle="modal" className="btn btn-sm" style={{marginRight: 15}}>Show terms</Button>
                    </ButtonGroup>
                    <h3><i className="fa fa-envelope"></i> <a href={messageUrl} style={{color: '#000'}}>{this.props.message.id}</a></h3>
                </Col>
            </Row>
            <Row>
                <Col md={3}>
                    <dl className="message-details">
                        <dt>Received by</dt>
                        <dd>
                            <em>Example Input</em> on <a href="#"><i className="fa fa-code-fork"></i> 127.0.0.1</a>
                        </dd>

                        <dt>Stored in index</dt>
                        <dd>@r.getIndex</dd>

                        <dt>Routed in streams</dt>
                        <dd>
                            <ul>
                                <li><a href="#">Some stream</a></li>
                                <li><a href="#">Example stream</a></li>
                                <li><a href="#">Another example stream</a></li>
                            </ul>
                        </dd>
                    </dl>
                </Col>
                <Col md={9}>
                    <div ref="messageList" style={{maxHeight: 1000, overflowY: 'auto'}}>
                        {this.state.scrollWarn ? <span><i className="fa fa-exclamation-triangle"></i> Scroll field list for more details...</span> : null}
                        <dl className="message-details message-details-fields">
                            {fields}
                        </dl>
                    </div>
                </Col>
            </Row>
        </div>);
    }
});

module.exports = MessageDetail;