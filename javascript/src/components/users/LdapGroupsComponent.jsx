const React = require('react');
const Immutable = require('immutable');

var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;
var Input = require('react-bootstrap').Input;
var Button = require('react-bootstrap').Button;

const RolesStore = require('../../stores/users/RolesStore').RolesStore;
const LdapGroupsStore = require('../../stores/users/LdapGroupsStore').LdapGroupsStore;

const LdapGroupsComponent = React.createClass({
  getInitialState() {
    return {
      groups: Immutable.Set.of(),
      roles: Immutable.Set.of(),
      mapping: Immutable.Map(),
    };
  },

  componentDidMount() {
    LdapGroupsStore.loadMapping().done(mapping => this.setState({mapping: Immutable.Map(mapping)}));
    LdapGroupsStore.loadGroups().done(groups => this.setState({groups: Immutable.Set(groups)}));
    RolesStore.loadRoles().done(roles => this.setState({roles: Immutable.Set(roles)}));
  },

  render() {
    const options = this.state.roles.sort().map(role => {
      return (<option key={role.name} value={role.name}>{role.name}</option>);
    });
    const content = this.state.groups.sort().map(group => {
      return (<li key={group}>
        {group}
        <Input data-group={group} type="select" value={this.state.mapping.get(group, '')} onChange={this._updateMapping}>
          <option value="">None</option>
          {options}
        </Input>
      </li>);
    });

    return (
    <Row>
      <Col md={12}>
        <ul>{content}</ul>
      </Col>
      <Col>
        <Button onClick={this._saveMapping}>Save</Button>
        <Button href={jsRoutes.controllers.UsersController.index().url}>Cancel</Button>
      </Col>
    </Row>
    );
  },

  _updateMapping(e) {
    const role = e.target.value;
    const group = e.target.getAttribute('data-group');
    if (role === "") {
      this.setState({mapping: this.state.mapping.delete(group)});
    } else {
      this.setState({mapping: this.state.mapping.set(group, role)});
    }
  },
  _saveMapping() {
    LdapGroupsStore.saveMapping(this.state.mapping.toJS());
  },
});

module.exports = LdapGroupsComponent;
