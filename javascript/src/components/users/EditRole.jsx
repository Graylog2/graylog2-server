'use strict';

var React = require('react');
var Immutable = require('immutable');
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;
var Button = require('react-bootstrap').Button;

var RolesStore = require('../../stores/users/RolesStore').RolesStore;
var PermissionsMixin = require('../../util/PermissionsMixin');

var EditRole = React.createClass({
    mixins: [PermissionsMixin],

    propTypes: {
        initialRole: React.PropTypes.object,
        onSave: React.PropTypes.func.isRequired,
        cancelEdit: React.PropTypes.func.isRequired
    },

    getInitialState() {
        return {
            role: this.props.initialRole,
            initialName: this.props.initialRole.name
        };
    },

    componentWillReceiveProps(newProps) {
        this.setState({role: newProps.initialRole, initialName: newProps.initialRole.name});
    },

    _setName(ev) {
        let role = this.state.role;
        role.name = ev.target.value;
        this.setState({role: this.state.role});
    },
    _setDescription(ev) {
        let role = this.state.role;
        role.description = ev.target.value;
        this.setState({role: this.state.role});
    },

    render() {
        return (
            <Row>
                <Col md={12}>
                    <form>
                        <label htmlFor="role-name">Name:</label>
                        <input id="role-name" type="text" className="form-control" onChange={this._setName} value={this.state.role.name} required/>

                        <label htmlFor="role-description">Description:</label>
                        <input id="role-description" type="text" className="form-control" onChange={this._setDescription} value={this.state.role.description}/>

                        <Button onClick={ev => this.props.onSave(this.state.initialName, this.state.role)}>Save</Button>
                        <Button onClick={this.props.cancelEdit}>Cancel</Button>
                    </form>
                </Col>
            </Row>);
    }
});

module.exports = EditRole;