/* global jsRoutes */

'use strict';

var $ = require('jquery');

var React = require('react');
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var Button = require('react-bootstrap').Button;
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;
var OverlayTrigger = require('react-bootstrap').OverlayTrigger;
var Tooltip = require('react-bootstrap').Tooltip;
var SplitButton = require('react-bootstrap').SplitButton;
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var Alert = require('react-bootstrap').Alert;
var ReactZeroClipboard = require('react-zeroclipboard');

var Immutable = require('immutable');
var MessagesStore = require('../../stores/messages/MessagesStore');
var StreamsStore = require('../../stores/streams/StreamsStore');

var MessageDetail = React.createClass({
    getInitialState() {
        return {
            messageTerms: Immutable.Map(),
            streamsListLoaded: false,
            streamsList: Immutable.List()
        };
    },
    componentDidMount() {
        this._loadStreams();
    },
    _inputName(inputId) {
        var input = this.props.inputs.get(inputId);
        return input ? <span style={{wordBreak: 'break-word'}}>{input['title']}</span> : "deleted input";
    },
    _nodeName(nodeId) {
        var node = this.props.nodes.get(nodeId);
        return node ?
            <a href={jsRoutes.controllers.NodesController.node(nodeId).url}>
                <i className="fa fa-code-fork"></i>
                &nbsp;
                <span style={{wordBreak: 'break-word'}}>{node['short_node_id']}</span>&nbsp;/&nbsp;<span style={{wordBreak: 'break-word'}}>{node['hostname']}</span>
            </a>
            :
            <span style={{wordBreak: 'break-word'}}>stopped node</span>;
    },

    _loadTerms(field) {
        return () => {
            var promise = MessagesStore.fieldTerms(this.props.message.index, this.props.message.id, field);
            promise.done((terms) => this._onTermsLoaded(field, terms));
        };
    },
    _onTermsLoaded(field, terms) {
        var map = Immutable.Map().set(field, terms);
        this.setState({messageTerms: map});
    },

    _loadStreams() {
        // only load the streams once per message.
        if (this.state.streamsListLoaded) {
            return;
        }
        var promise = StreamsStore.listStreams();
        promise.done((streams) => this._onStreamsLoaded(streams));
    },
    _onStreamsLoaded(streams) {
        this.setState({streamsListLoaded: true, streamsList: Immutable.List(streams).sortBy(stream => stream.title)});
    },
    render() {
        var messageUrl = jsRoutes.controllers.MessagesController.show(this.props.message.index, this.props.message.id).url;

        var fields = [];
        var formattedFields = Immutable.Map(this.props.message['formatted_fields']).sortBy((value, key) => key, (a,b) => a.localeCompare(b));
        var idx = 0;
        formattedFields.forEach((value, key) => {
            idx++;
            var showTerms = this.state.messageTerms.has(key);
            var terms = showTerms ? Immutable.fromJS(this.state.messageTerms.get(key)) : Immutable.List();
            var termsMarkup = [];
            terms.forEach((term, idx) => termsMarkup.push(<span key={idx} className="message-terms">{term}</span>));
            fields.push(<dt key={key + "dt"}>{key}</dt>);
            fields.push(
                <dd key={key + "dd"}>
                    <div className="message-field-actions pull-right">
                        <SplitButton pullRight={true} bsSize="xsmall" title={<i className="fa fa-search-plus"></i>} key={1} onClick={() => $(document).trigger('add-search-term.graylog.universalsearch', {field: key, value: value})}>
                            <li className="dropdown-submenu left-submenu">
                                <a href="#">Create extractor for field {key}</a>
                                <ul className="dropdown-menu">
                                    <li><a href={jsRoutes.controllers.ExtractorsController.newExtractor(this.props.message['source_node_id'], this.props.message['source_input_id'], "regex", key, this.props.message.index, this.props.message.id).url}>Regular expression</a></li>
                                    <li><a href={jsRoutes.controllers.ExtractorsController.newExtractor(this.props.message['source_node_id'], this.props.message['source_input_id'], "substring", key, this.props.message.index, this.props.message.id).url}>Substring</a></li>
                                    <li><a href={jsRoutes.controllers.ExtractorsController.newExtractor(this.props.message['source_node_id'], this.props.message['source_input_id'], "split_and_index", key, this.props.message.index, this.props.message.id).url}>Split &amp; Index</a></li>
                                    <li><a href={jsRoutes.controllers.ExtractorsController.newExtractor(this.props.message['source_node_id'], this.props.message['source_input_id'], "copy_input", key, this.props.message.index, this.props.message.id).url}>Copy Input</a></li>
                                    <li><a href={jsRoutes.controllers.ExtractorsController.newExtractor(this.props.message['source_node_id'], this.props.message['source_input_id'], "grok", key, this.props.message.index, this.props.message.id).url}>Grok pattern</a></li>
                                </ul>
                            </li>
                            <MenuItem onClick={this._loadTerms(key)}>Show terms of {key}</MenuItem>
                        </SplitButton>
                    </div>
                    {value}
                    {showTerms && <br />}
                    {showTerms && <Alert bsStyle='info' onDismiss={() => this.setState({messageTerms: Immutable.Map()})}>Field terms: {termsMarkup}</Alert>}
                </dd>
            );
        });

        var streamList = null;
        this.state.streamsList.forEach((stream) => {
            if (!streamList) {
                streamList = [];
            }
            var url = jsRoutes.controllers.StreamRulesController.index(stream['id']).url + "#" + this.props.message.id + "." + this.props.message.index;

            streamList.push(<MenuItem key={stream['id']} href={url}>{stream['title']}</MenuItem>);
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

                        <DropdownButton ref="streamDropdown" pullRight bsSize="small" title="Test against stream">
                            { streamList }
                            { ! streamList && <MenuItem header><i className="fa fa-spin fa-spinner"></i> Loading streams</MenuItem> }
                        </DropdownButton>
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
                        <dd className="stream-list">
                            <ul>
                                {streams}
                            </ul>
                        </dd>
                        }
                    </dl>
                </Col>
                <Col md={9}>
                    <div ref="messageList">
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