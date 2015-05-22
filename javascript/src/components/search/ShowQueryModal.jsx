'use strict';

var React = require('react');
var PureRenderMixin = require('react/addons').addons.PureRenderMixin;

var Modal = require('react-bootstrap').Modal;
var Button = require('react-bootstrap').Button;
var OverlayTrigger = require('react-bootstrap').OverlayTrigger;
var Tooltip = require('react-bootstrap').Tooltip;
var ReactZeroClipboard = require('react-zeroclipboard');

var ShowQueryModal = React.createClass({
    mixins: [PureRenderMixin],

    propTypes: {
        builtQuery: React.PropTypes.string,
        onRequestHide: React.PropTypes.func
    },

    render () {
        var queryText = JSON.stringify(JSON.parse(this.props.builtQuery), null, '  ');
        return <Modal title='Elasticsearch Query' onRequestHide={this.props.onRequestHide}>
            <div className="modal-body">
                <pre>{queryText}</pre>
            </div>
            <div className="modal-footer">
                <OverlayTrigger
                    placement="top"
                    ref="copyBtnTooltip"
                    overlay={<Tooltip>Query copied to clipboard.</Tooltip>}>
                    <ReactZeroClipboard
                        text={queryText}
                        onAfterCopy={() => { this.refs['copyBtnTooltip'].toggle(); window.setTimeout(() => this.refs['copyBtnTooltip'] && this.refs['copyBtnTooltip'].toggle(), 1000); } }>
                        <Button>Copy query</Button>
                    </ReactZeroClipboard>
                </OverlayTrigger>
            </div>
        </Modal>;
    }
});

module.exports = ShowQueryModal;