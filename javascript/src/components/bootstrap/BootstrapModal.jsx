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
        modal.on("shown.bs.modal", () => {
            var element = $("input", this.refs.body.getDOMNode()).first();

            if (element.length === 0) {
                element = $("input, button", this.refs.footer.getDOMNode()).first();
            }

            element.focus();
        });
    },
    _submit(event) {
        this.props.onConfirm(event);
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
                <button type="button" className="btn" onClick={this.props.onCancel}>
                      {this.props.cancel}
                </button>
                );
        }
        var formContent = (
            <div className="modal-content">
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
            <div ref="modal" className="modal fade" aria-hidden="true" role="dialog" tabIndex="-1">
                <div className="modal-dialog">
                    {form}
                </div>
            </div>
            );
    }
});

module.exports = BootstrapModal;
