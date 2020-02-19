import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';
import styled from 'styled-components';
import { Button, Col, Panel, Row } from 'components/graylog';

import { Input } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { naturalSortIgnoreCase } from 'util/SortUtils';

import CombinedProvider from 'injection/CombinedProvider';

const { LdapGroupsActions } = CombinedProvider.get('LdapGroups');
const { RolesStore } = CombinedProvider.get('Roles');

const StyledLegend = styled.legend`
  font-size: 1.5em;
`;

class LdapGroupsComponent extends React.Component {
  static propTypes = {
    onCancel: PropTypes.func.isRequired,
    onShowConfig: PropTypes.func.isRequired,
  };

  state = {
    groups: undefined,
    roles: undefined,
    mapping: undefined,
    groupsErrorMessage: null,
  };

  componentDidMount() {
    LdapGroupsActions.loadMapping().then(mapping => this.setState({ mapping: Immutable.Map(mapping) }));
    LdapGroupsActions.loadGroups()
      .then(
        groups => this.setState({ groups: Immutable.Set(groups) }),
        (error) => {
          if (error.additional.status !== 400) {
            this.setState({ groupsErrorMessage: error });
          }
        },
      );
    RolesStore.loadRoles().then(roles => this.setState({ roles: Immutable.Set(roles) }));
  }

  _updateMapping = (event) => {
    const role = event.target.value;
    const group = event.target.getAttribute('data-group');
    const { mapping } = this.state;
    this.setState({ mapping: mapping.set(group, role) });
  };

  _saveMapping = (event) => {
    event.preventDefault();
    const { mapping } = this.state;
    LdapGroupsActions.saveMapping(mapping.filter(role => role !== '').toJS());
  };

  _onShowConfig = () => {
    const { onShowConfig } = this.props;
    onShowConfig();
  };

  _isLoading = () => {
    const { groups, mapping, roles } = this.state;
    return !(mapping && groups && roles);
  };

  _renderGroupMappingInputs = (groups, options) => {
    const { mapping } = this.state;

    return groups
      .sort(naturalSortIgnoreCase)
      .map((group) => {
        return (
          <Input id={`${group}-select`}
                 key={`${group}-select`}
                 label={group}
                 data-group={group}
                 type="select"
                 value={mapping.get(group, '')}
                 onChange={this._updateMapping}
                 labelClassName="col-sm-2"
                 wrapperClassName="col-sm-5">
            <option value="">None</option>
            {options}
          </Input>
        );
      });
  };

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }
    const { groups, groupsErrorMessage, mapping, roles } = this.state;
    const { onCancel } = this.props;

    if (groupsErrorMessage) {
      return (
        <Panel header="Error: Unable to load LDAP groups" bsStyle="danger">
          The error message was:<br />{groupsErrorMessage.message}
        </Panel>
      );
    }

    if (groups.size === 0 && mapping.size === 0) {
      return (
        <Panel header="No LDAP/Active Directory groups found" bsStyle="info">
          <p>Please verify that your LDAP group mapping settings are correct.</p>
          <Button bsSize="sm" onClick={this._onShowConfig}>Open LDAP settings</Button>
        </Panel>
      );
    }

    const options = roles.sort((r1, r2) => naturalSortIgnoreCase(r1.name, r2.name)).map((role) => {
      return <option key={role.name} value={role.name}>{role.name}</option>;
    });

    const currentLdapSearchGroups = groups;
    const previousMappings = Immutable.Set(mapping.keySeq()).filter(group => !groups.contains(group));

    return (
      <form className="form-horizontal" onSubmit={this._saveMapping}>
        <Row>
          <Col md={12}>
            <StyledLegend>Group mapping from LDAP/Active Directory</StyledLegend>
            <p>Assign Graylog roles to LDAP/Active Directory groups.</p>
            {currentLdapSearchGroups.size === 0
              ? 'No LDAP/Active Directory groups found, please verify your LDAP group mapping settings.'
              : this._renderGroupMappingInputs(currentLdapSearchGroups, options)}

            {previousMappings.size > 0 && (
              <>
                <StyledLegend>Previously configured group mapping</StyledLegend>
                <p>
                  Some LDAP/Active Directory groups not matching your current settings were previously assigned Graylog
                  roles. <strong>This mapping is still active for users logging into Graylog until you remove it.</strong>
                </p>
                {this._renderGroupMappingInputs(previousMappings, options)}
              </>
            )}
          </Col>
          <Col md={10} mdPush={2}>
            <Button type="submit" bsStyle="primary" className="save-button-margin">Save</Button>
            <Button onClick={onCancel}>Cancel</Button>
          </Col>
        </Row>
      </form>
    );
  }
}

export default LdapGroupsComponent;
