/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import lodash from 'lodash';

import { Row, Col } from 'components/graylog';
import { BootstrapModalConfirm } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';

import ContentPackEntitiesList from './ContentPackEntitiesList';
import ContentPackParameterList from './ContentPackParameterList';

class ContentPackParameters extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onStateChange: PropTypes.func,
    appliedParameter: PropTypes.object.isRequired,
  };

  static defaultProps = {
    onStateChange: () => {},
  };

  static emptyParameter = {
    name: '',
    title: '',
    description: '',
    type: 'string',
    default_value: '',
  };

  constructor(props) {
    super(props);

    this.state = {
      newParameter: ObjectUtils.clone(ContentPackParameters.emptyParameter),
      parameterToDelete: undefined,
      defaultValueError: undefined,
      nameError: undefined,
    };
  }

  _addNewParameter = (newParameter, oldParameter) => {
    let newContentPackBuilder = this.props.contentPack.toBuilder();
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);

    if (oldParameter) {
      /* If the name of the parameter changed we need to update the reference in appliedParameter */
      Object.keys(newAppliedParameter).forEach((id) => {
        newAppliedParameter[id] = newAppliedParameter[id].map((paramMap) => {
          if (paramMap.paramName === oldParameter.name) {
            return { configKey: paramMap.configKey, paramName: newParameter.name };
          }

          return paramMap;
        });
      });

      /* If we update a parameter we remove the old one first */
      newContentPackBuilder = newContentPackBuilder.removeParameter(oldParameter);
    }

    newContentPackBuilder.addParameter(newParameter);
    this.props.onStateChange({ contentPack: newContentPackBuilder.build(), appliedParameter: newAppliedParameter });
  };

  _onParameterApply = (id, configKey, paramName) => {
    const paramMap = { configKey: configKey, paramName: paramName };
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);

    newAppliedParameter[id] = newAppliedParameter[id] || [];
    newAppliedParameter[id].push(paramMap);
    this.props.onStateChange({ appliedParameter: newAppliedParameter });
  };

  _onParameterClear = (id, configKey) => {
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);

    lodash.remove(newAppliedParameter[id], (paramMap) => { return paramMap.configKey === configKey; });
    this.props.onStateChange({ appliedParameter: newAppliedParameter });
  };

  _deleteParameter = (parameter) => {
    const { contentPack } = this.props;
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);

    /* If we delete a parameter we need to remove the reference from appliedParameter */
    Object.keys(newAppliedParameter).forEach((id) => {
      lodash.remove(newAppliedParameter[id], (paramMap) => { return paramMap.paramName === parameter.name; });

      if (newAppliedParameter[id].length <= 0) {
        delete newAppliedParameter[id];
      }
    });

    const newContentPack = contentPack.toBuilder().removeParameter(parameter).build();

    this.props.onStateChange({ contentPack: newContentPack, appliedParameter: newAppliedParameter });
    this._closeConfirmModal();
  };

  _confirmationModal = () => {
    return (
      <BootstrapModalConfirm ref={(c) => { this.modal = c; }}
                             title="Confirm deletion"
                             onConfirm={() => { this._deleteParameter(this.state.parameterToDelete); }}
                             onCancel={this._closeConfirmModal}>
        {`Are you sure you want to do delete this parameter: ${(this.state.parameterToDelete || {}).title}?`}
      </BootstrapModalConfirm>
    );
  };

  _openConfirmModal = (parameter) => {
    this.setState({ parameterToDelete: parameter });
    this.modal.open();
  };

  _closeConfirmModal = () => {
    this.setState({ parameterToDelete: undefined });
    this.modal.close();
  };

  render() {
    return (
      <div>
        <Row>
          <Col smOffset={1} sm={9}>
            <ContentPackParameterList contentPack={this.props.contentPack}
                                      onAddParameter={this._addNewParameter}
                                      onDeleteParameter={this._openConfirmModal}
                                      appliedParameter={this.props.appliedParameter} />
            {this._confirmationModal()}
          </Col>
        </Row>
        <Row>
          <Col smOffset={1} sm={9}>
            <ContentPackEntitiesList contentPack={this.props.contentPack}
                                     onParameterClear={this._onParameterClear}
                                     onParameterApply={this._onParameterApply}
                                     appliedParameter={this.props.appliedParameter} />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackParameters;
