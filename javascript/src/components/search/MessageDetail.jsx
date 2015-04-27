/* global jsRoutes */

'use strict';

var React = require('react');
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var Button = require('react-bootstrap').Button;
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;
var OverlayTrigger = require('react-bootstrap').OverlayTrigger;
var Tooltip = require('react-bootstrap').Tooltip;
var ReactZeroClipboard = require('react-zeroclipboard');

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
    _inputName(inputId) {
        var input = this.props.inputs.get(inputId);
        return input ? <span style={{wordBreak: 'break-word'}}>{input['title']}</span> : "deleted input";
    },
    _nodeName(nodeId) {
        var node = this.props.nodes.get(nodeId);
        return node
            ?
            <a href={jsRoutes.controllers.NodesController.node(nodeId).url}>
                <i className="fa fa-code-fork"></i>
                &nbsp;
                <span style={{wordBreak: 'break-word'}}>{node['short_node_id']}</span>&nbsp;/&nbsp;<span style={{wordBreak: 'break-word'}}>{node['hostname']}</span>
            </a>
            :
            <span style={{wordBreak: 'break-word'}}>stopped node</span>;
    },
    render() {
        var messageUrl = jsRoutes.controllers.MessagesController.show(this.props.message.index, this.props.message.id).url;

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

        var streamIds = Immutable.Set(this.props.message['stream_ids']);
        var streams = streamIds
            .map((id) => this.props.streams.get(id))
            .map((stream) => <li key={stream.id}><a href="#">{stream.title}</a></li>);

        var viaRadio = this.props.message['source_radio_id'];
        if (viaRadio) {
            viaRadio = <span>
                via <em>{this._inputName(this.props.message['source_radio_input_id'])}</em> on radio {this._nodeName(this.props.message['source_radio_id'])}
            </span>;
        }
        return (<div>

            <Row>
                <Col md={12}>
                    <ButtonGroup className="pull-right" bsSize="small" style={{marginRight: 15}}>
                        <Button href={messageUrl}>Permalink</Button>

                        <OverlayTrigger
                            placement="top"
                            ref="copyBtnTooltip"
                            overlay={<Tooltip>Message ID copied to clipboard.</Tooltip>}>
                            <ReactZeroClipboard
                                text={this.props.message.id}
                                onAfterCopy={() => { this.refs['copyBtnTooltip'].toggle(); window.setTimeout(() => this.refs['copyBtnTooltip'].toggle(), 1000); } }>
                                <Button>Copy ID</Button>
                            </ReactZeroClipboard>
                        </OverlayTrigger>

                        <Button href="#">Show terms</Button>

                        <Button href="#">Test against stream</Button>
                    </ButtonGroup>
                    <h3><i className="fa fa-envelope"></i> <a href={messageUrl} style={{color: '#000'}}>{this.props.message.id}</a></h3>
                </Col>
            </Row>
            <Row>
                <Col md={3}>
                    <dl className="message-details">
                        <dt>Received by</dt>
                        <dd>
                            <em>{this._inputName(this.props.message['source_input_id'])}</em> on {this._nodeName(this.props.message['source_node_id'])}
                            { viaRadio && <br /> }
                            {viaRadio}
                        </dd>

                        <dt>Stored in index</dt>
                        <dd>{this.props.message.index}</dd>

                        { streamIds.size > 0 && <dt>Routed into streams</dt> }
                        { streamIds.size > 0 &&
                        <dd>
                            <ul>
                                {streams}
                            </ul>
                        </dd>
                        }
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