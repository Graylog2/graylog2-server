'use strict';

var React = require('react/addons');
var Modal = require('react-bootstrap').Modal;
var OverlayMixin = require('react-bootstrap').OverlayMixin;

var $ = require('jquery'); // excluded and shimed

var BootstrapModalTrigger = React.createClass({
    mixins: [OverlayMixin],

    getInitialState() {
        return {
            isModalOpen: false
        };
    },
    _modalShown() {
        if (this.state.isModalOpen && typeof this.props.onShown === 'function') {
            this.props.onShown();
        }
    },
    open() {
        this.setState({isModalOpen: true}, this._modalShown);
    },
    close() {
        this.setState({isModalOpen: false});
        if (typeof this.props.onRequestHide === 'function') {
            this.props.onRequestHide();
        }
    },
    render() {
        return <span/>;
    },
    // This is called by the `OverlayMixin` when this component
    // is mounted or updated and the return value is appended to the body.
    renderOverlay() {
        if (!this.state.isModalOpen) {
            return <span/>;
        }

        return (
            <Modal onRequestHide={this.close}>
                {this.props.children}
            </Modal>
        );
    }
});

// adapted from react examples (https://github.com/facebook/react/tree/master/examples/jquery-bootstrap)
var BootstrapModal = React.createClass({
    close() {
        this.refs.modal.close();
    },
    open() {
        if (window.event) {
            window.event.preventDefault();
        }
        this.refs.modal.open();
    },
    _onModalShown() {
        this._focusFirstInput();
        if (typeof this.props.onShown === 'function') {
            this.props.onShown();
        }
    },
    _focusFirstInput() {
        var element = $("input[type!=hidden],select,textarea", React.findDOMNode(this.refs.body)).first();

        if (element.length === 0) {
            element = $("input, button", React.findDOMNode(this.refs.footer)).first();
        }

        element.focus();
    },
    _submit(event) {
        event.target.checkValidity();
        this.props.onConfirm(event);
        event.preventDefault();
    },
    render() {
        var confirmButton = null;
        var cancelButton = null;

        if (this.props.confirm && this.props.onConfirm) {
            confirmButton = <button type="submit" className="btn btn-primary">{this.props.confirm}</button>;
        }
        if (this.props.cancel && this.props.onCancel) {
            cancelButton = (
                <button type="button" className="btn" onClick={this.props.onCancel}>
                    {this.props.cancel}
                </button>
            );
        }
        var formContent = (
            <div>
                <div className="modal-header">
                    <button
                        type="button"
                        className="close"
                        data-dismiss="modal"
                        aria-label="Close"
                        onClick={this.props.onCancel}
                        dangerouslySetInnerHTML={{__html: '&times'}}
                        />
                    {Array.isArray(this.props.children) ? this.props.children[0] : this.props.children}
                </div>
                <div ref="body" className="modal-body">
                    <div className="container-fluid">
                        {this.props.children[1]}
                    </div>
                </div>
                <div ref="footer" className="modal-footer">
                    {cancelButton}
                    {confirmButton}
                </div>
            </div>
        );
        var form = (
            <form
                onSubmit={this._submit}
                className={this.props.formClass}
                encType={this.props.encType}
                method={this.props.method}
                action={this.props.action}>
                {formContent}
            </form>
        );

        return (
            <BootstrapModalTrigger ref="modal"
                                   onShown={this._onModalShown}
                                   onRequestHide={this.props.onHidden}>
                {form}
            </BootstrapModalTrigger>
        );
    }
});

module.exports = BootstrapModal;
