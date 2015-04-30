'use strict';

var React = require('react/addons');
var BootstrapModal = require('../bootstrap/BootstrapModal');
var Input = require('react-bootstrap').Input;
var Bubble = require('../support/Bubble');

var StreamRuleForm = React.createClass({
    mixins: [React.addons.LinkedStateMixin],
    getInitialState() {
        return this.props.streamRule;
    },
    getDefaultProps() {
        return {
            streamRule: {field: "", type: "", value: "", inverted: false}
        };
    },
    _resetValues() {
        this.setState(this.props.streamRule);
    },
    _onSubmit(evt) {
        this.props.onSubmit(this.props.streamRule.id, this.state);
        this.refs.modal.close();
    },
    open() {
        this._resetValues();
        this.refs.modal.open();
    },
    close() {
        this.refs.modal.close();
    },
    componentDidMount() {
    },
    render() {
        return (
            <BootstrapModal ref='modal' onCancel={this.close} onConfirm={this._onSubmit} cancel="Cancel" confirm="Save">
                <div>
                    <h2>{this.props.title}</h2>
                </div>
                <div>
                    <div className='col-md-8'>
                        <Input type='text' required={true} label='Field' placeholder='user_id' valueLink={this.linkState('field')}/>
                        <Input type='select' required={true} label='Type' valueLink={this.linkState('type')}>
                        </Input>
                        <Input type='text' required={true} label='Value' placeholder='19983' valueLink={this.linkState('value')}/>
                        <Input type='checkbox' required={true} label='Inverted' valueLink={this.linkState('inverted')}/>

                        <p>
                            <strong>Result:</strong>
                            <span id="sr-result">
                                Field <em ref="result-field">user_id</em> must
                                <em ref="result-category">@StreamRule.Type.fromInt(1).getLongDesc()</em>
                                <em ref="result-value">19983</em>
                            </span>
                        </p>
                    </div>
                    <div className='col-md-4'>
                        <div className="well well-small matcher-github">
                            The server will try to convert to strings or numbers based on the matcher type as good as it can.

                            <br /><br />
                            <i className="fa fa-github"></i>
                            <a href="https://github.com/Graylog2/graylog2-server/tree/@Version.VERSION.getBranchName/graylog2-server/src/main/java/org/graylog2/streams/matchers" target="_blank">
                                Take a look at the matcher code on GitHub
                            </a>
                            <br /><br />
                            Regular expressions use Java syntax. <Bubble link="general/streams" />
                        </div>
                    </div>
                </div>
            </BootstrapModal>
        );
    }
});

module.exports = StreamRuleForm;
