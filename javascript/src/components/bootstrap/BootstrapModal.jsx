'use strict';

var React = require('react/addons');
var $ = require('jquery'); // excluded and shimed

// adapted from react examples (https://github.com/facebook/react/tree/master/examples/jquery-bootstrap)
var BootstrapModal = React.createClass({
    componentDidMount() {
        this._modalNode()
            .modal({backdrop: 'static', keyboard: true, show: false});
    },
    componentWillUnmount() {
    },
    close() {
        this._modalNode().modal('hide');
    },
    open() {
        var modal = this._modalNode();
        modal.modal('show');
        modal.on("shown", () => $("input", this.refs.body.getDOMNode()).first().focus());
    },
    _submit(event) {
        this.props.onConfirm();
        event.preventDefault();
    },
    _modalNode() {
        return $(this.refs.modal.getDOMNode());
    },
    render() {
        var confirmButton = null;
        var cancelButton = null;

        if (this.props.confirm && this.props.onConfirm) {
            confirmButton = <input role="button" value={this.props.confirm} type="submit" className="btn btn-primary"/>;
        }
        if (this.props.cancel && this.props.onCancel) {
            cancelButton = (
                <a role="button" className="btn" onClick={this.props.onCancel}>
                      {this.props.cancel}
                </a>
                );
        }

        return (
            <div ref="modal" className="modal hide fade">
                <form  onSubmit={this._submit} className={this.props.formClass}>
                    <div className="modal-header">
                        <button
                        type="button"
                        className="close"
                        data-dismiss="modal"
                        onClick={this.props.onCancel}
                        dangerouslySetInnerHTML={{__html: '&times'}}
                        />
                            {Array.isArray(this.props.children) ? this.props.children[0] : this.props.children}
                    </div>
                    <div ref="body" className="modal-body">
                            {this.props.children[1]}
                    </div>
                    <div className="modal-footer">
                          {cancelButton}
                          {confirmButton}
                    </div>
                </form>
            </div>
            );
    }
});

module.exports = BootstrapModal;
