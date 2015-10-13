'use strict';

var React = require('react');
var PureRenderMixin = require('react-addons-pure-render-mixin');

var Modal = require('react-bootstrap').Modal;
var Button = require('react-bootstrap').Button;
var OverlayTrigger = require('react-bootstrap').OverlayTrigger;
var Tooltip = require('react-bootstrap').Tooltip;
var ReactZeroClipboard = require('react-zeroclipboard');

var BootstrapModalWrapper = require('../bootstrap/BootstrapModalWrapper');

var ShowQueryModal = React.createClass({
    mixins: [PureRenderMixin],

    propTypes: {
        builtQuery: React.PropTypes.string,
    },

    open() {
        this.refs.modal.open();
    },

    close() {
        this.refs.modal.close();
    },

    render () {
        var queryText = JSON.stringify(JSON.parse(this.props.builtQuery), null, '  ');
        return (
          <BootstrapModalWrapper ref="modal">
              <Modal.Header closeButton>
                  <Modal.Title>Elasticsearch Query</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                  <pre>{queryText}</pre>
              </Modal.Body>
              <Modal.Footer>
                  <OverlayTrigger
                    placement="top"
                    ref="copyBtnTooltip"
                    overlay={<Tooltip id="elasticsearch-query-copied-tooltip">Query copied to clipboard.</Tooltip>}>
                      <ReactZeroClipboard
                        text={queryText}
                        onAfterCopy={() => { this.refs['copyBtnTooltip'].toggle(); window.setTimeout(() => this.refs['copyBtnTooltip'] && this.refs['copyBtnTooltip'].toggle(), 1000); } }>
                          <Button>Copy query</Button>
                      </ReactZeroClipboard>
                  </OverlayTrigger>
              </Modal.Footer>
          </BootstrapModalWrapper>
        );
    }
});

module.exports = ShowQueryModal;