'use strict';

var React = require('react/addons');
var BootstrapModal = require('../bootstrap/BootstrapModal');
var Input = require('react-bootstrap').Input;
var Bubble = require('../support/Bubble');
var HumanReadableStreamRule = require('./HumanReadableStreamRule');
var Col = require('react-bootstrap').Col;
var TypeAheadFieldInput = require('../common/TypeAheadFieldInput');

var StreamRuleForm = React.createClass({
    mixins: [React.addons.LinkedStateMixin],
    FIELD_PRESENCE_RULE_TYPE: 5,
    getInitialState() {
        return this.props.streamRule;
    },
    getDefaultProps() {
        return {
            streamRule: {field: "", type: 1, value: "", inverted: false}
        };
    },
    _resetValues() {
        this.setState(this.props.streamRule);
    },
    _onSubmit(evt) {
        if (this.state.type === this.FIELD_PRESENCE_RULE_TYPE) {
            this.state.value = "";
        }
        this.props.onSubmit(this.props.streamRule.id, this.state);
        this.refs.modal.close();
    },
    _formatStreamRuleType(streamRuleType) {
        return <option key={'streamRuleType'+streamRuleType.id}
                       value={streamRuleType.id}>{streamRuleType.short_desc}</option>;
    },
    _focusInput() {
        this.refs.modal.focusFirstInput();
    },
    open() {
        this._resetValues();
        this.refs.modal.open();
    },
    close() {
        this.refs.modal.close();
    },
    render() {
        var streamRuleTypes = this.props.streamRuleTypes.map(this._formatStreamRuleType);
        var valueBox = (String(this.state.type) !== String(this.FIELD_PRESENCE_RULE_TYPE) ?
            <Input type='text' required={true} label='Value' valueLink={this.linkState('value')}/> : "");
        return (
            <BootstrapModal ref='modal' onCancel={this.close} onConfirm={this._onSubmit} cancel="Cancel" confirm="Save">
                <div>
                    <h2>{this.props.title}</h2>
                </div>
                <div>
                    <Col md={8}>
                        <TypeAheadFieldInput ref='fieldInput'
                                             type='text'
                                             required={true}
                                             label='Field'
                                             valueLink={this.linkState('field')}
                                             onTypeaheadLoaded={this._focusInput}/>
                        <Input type='select' required={true} label='Type' valueLink={this.linkState('type')}>
                            {streamRuleTypes}
                        </Input>
                        {valueBox}
                        <Input type='checkbox' label='Inverted' checkedLink={this.linkState('inverted')}/>

                        <p>
                            <strong>Result:</strong>
                            {' '}
                            Field <HumanReadableStreamRule streamRule={this.state}
                                                           streamRuleTypes={this.props.streamRuleTypes}/>
                        </p>
                    </Col>
                    <Col md={4}>
                        <div className="well well-small matcher-github">
                            The server will try to convert to strings or numbers based on the matcher type as good as it
                            can.

                            <br /><br />
                            <i className="fa fa-github"></i>
                            <a href="https://github.com/Graylog2/graylog2-server/tree/@Version.VERSION.getBranchName/graylog2-server/src/main/java/org/graylog2/streams/matchers"
                               target="_blank">
                                Take a look at the matcher code on GitHub
                            </a>
                            <br /><br />
                            Regular expressions use Java syntax. <Bubble link="general/streams"/>
                        </div>
                    </Col>
                </div>
            </BootstrapModal>
        );
    }
});

module.exports = StreamRuleForm;
