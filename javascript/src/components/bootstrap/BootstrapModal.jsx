/* globals validate */

'use strict';

var React = require('react');
var Modal = require('react-bootstrap').Modal;
var BootstrapModalWrapper = require('./BootstrapModalWrapper');

var $ = require('jquery');

var BootstrapModal = React.createClass({
    close() {
        this.refs.modal.close();
    },
    open() {
        this.refs.modal.open();
    },
    _onModalShown() {
        this.focusFirstInput();
        if (typeof this.props.onShown === 'function') {
            this.props.onShown();
        }
    },
    focusFirstInput() {
        var element = $("input[type!=hidden],select,textarea", React.findDOMNode(this.refs.body)).not('.tt-hint').first();

        if (element.length === 0) {
            element = $("input, button", React.findDOMNode(this.refs.footer)).first();
        }

        element.focus();
    },
    _submit(event) {
        var formDOMNode = React.findDOMNode(this.refs.form);
        var $formDOMNode = $(formDOMNode);

        if ((typeof formDOMNode.checkValidity === 'function' && !formDOMNode.checkValidity()) ||
            (typeof $formDOMNode.checkValidity === 'function' && !$formDOMNode.checkValidity())) {
            event.preventDefault();
            return;
        }

        // Check custom validation for plugin fields
        if (!validate(formDOMNode)) {
            event.preventDefault();
            return;
        }

        if (typeof this.props.onConfirm === 'function') {
            event.preventDefault();
            this.props.onConfirm(event);
        }
    },
    render() {
        var confirmButton = null;
        var cancelButton = null;

        if (this.props.confirm) {
            confirmButton = <button type="submit" className="btn btn-primary">{this.props.confirm}</button>;
        }
        if (this.props.cancel && this.props.onCancel) {
            cancelButton = (
                <button type="button" className="btn" onClick={this.props.onCancel}>
                    {this.props.cancel}
                </button>
            );
        }

        return (
            <BootstrapModalWrapper ref="modal" onOpen={this._onModalShown} onClose={this.props.onHidden}>
                <form ref="form"
                      onSubmit={this._submit}
                      className={this.props.formClass}
                      encType={this.props.encType}
                      method={this.props.method}
                      action={this.props.action}>
                    <Modal.Header closeButton>
                        {Array.isArray(this.props.children) ? this.props.children[0] : this.props.children}
                    </Modal.Header>
                    <Modal.Body ref="body">
                        <div className="container-fluid">
                            {this.props.children[1]}
                        </div>
                    </Modal.Body>
                    <Modal.Footer ref="footer">
                        {cancelButton}
                        {confirmButton}
                    </Modal.Footer>
                </form>
            </BootstrapModalWrapper>
        );
    }
});

module.exports = BootstrapModal;
