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
import React from 'react';
import findIndex from 'lodash/findIndex';

import i18n from 'i18n';
import { Button, Modal, ButtonToolbar } from 'components/bootstrap';
import { DataTable, SearchForm, ModalSubmit, StatusIcon } from 'components/common';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import ContentPackEditParameter from 'components/content-packs/ContentPackEditParameter';
import ObjectUtils from 'util/ObjectUtils';

import ContentPackParameterListStyle from './ContentPackParameterList.css';
import ContentPackUtils from './ContentPackUtils';

const t = (key: string) => i18n.t(key, { ns: 'content_packs' });

type ContentPackParameterListProps = {
  contentPack: any;
  readOnly?: boolean;
  onDeleteParameter?: (...args: any[]) => void;
  onAddParameter?: (...args: any[]) => void;
  appliedParameter?: any;
};

class ContentPackParameterList extends React.Component<
  ContentPackParameterListProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    readOnly: false,
    onDeleteParameter: () => {},
    onAddParameter: () => {},
    appliedParameter: {},
  };

  constructor(props) {
    super(props);

    this.state = {
      openModalKey: undefined,
      filteredParameters: props.contentPack.parameters || [],
      filter: undefined,
    };
  }

  UNSAFE_componentWillReceiveProps(newProps) {
    const { filter } = this.state;

    this._filterParameters(filter, newProps.contentPack.parameters);
  }

  _parameterApplied = (paramName) => {
    const { appliedParameter, contentPack } = this.props;
    // appliedParameter can keep refs for deselected or swapped-out entities, so only count current ones.
    const currentEntityIds = new Set((contentPack.entities || []).map((entity) => entity.id));

    return Object.keys(appliedParameter)
      .filter((id) => currentEntityIds.has(id))
      .some((id) => findIndex(appliedParameter[id], { paramName: paramName }) >= 0);
  };

  _parameterRowFormatter = (parameter) => {
    const { onDeleteParameter, readOnly } = this.props;
    const parameterApplied = this._parameterApplied(parameter.name);
    const buttonTitle = parameterApplied ? t('parameter_list.still_in_use') : t('parameter_list.delete_parameter');

    return (
      <tr key={parameter.title}>
        <td className={ContentPackParameterListStyle.bigColumns}>{parameter.title}</td>
        <td>{parameter.name}</td>
        <td className={ContentPackParameterListStyle.bigColumns}>{parameter.description}</td>
        <td>{parameter.type}</td>
        <td>{ContentPackUtils.convertToString(parameter)}</td>
        <td>
          <StatusIcon active={parameterApplied} />
        </td>
        {!readOnly && (
          <td>
            <ButtonToolbar>
              <Button
                bsStyle="danger"
                bsSize="xs"
                title={buttonTitle}
                disabled={parameterApplied}
                onClick={() => {
                  onDeleteParameter(parameter);
                }}>
                {t('common:actions.delete')}
              </Button>
              {this._parameterModal(parameter)}
            </ButtonToolbar>
          </td>
        )}
      </tr>
    );
  };

  _filterParameters = (filter, parametersArg?) => {
    const { contentPack } = this.props;
    const parameters = ObjectUtils.clone(parametersArg || contentPack.parameters);

    if (!filter || filter.length <= 0) {
      this.setState({ filteredParameters: parameters, filter: undefined });

      return;
    }

    const regexp = RegExp(filter, 'i');
    const filteredParameters = parameters.filter(
      (parameter) => regexp.test(parameter.title) || regexp.test(parameter.description) || regexp.test(parameter.name),
    );

    this.setState({ filteredParameters: filteredParameters, filter: filter });
  };

  _parameterModal(parameter?) {
    let editParameter;

    const { contentPack, onAddParameter } = this.props;
    const { openModalKey } = this.state;
    // Parameter names are never empty, so '' identifies the create modal
    const modalKey = parameter ? parameter.name : '';

    const closeModal = () => {
      this.setState({ openModalKey: undefined });
    };

    const openModal = () => {
      this.setState({ openModalKey: modalKey });
    };

    const addParameter = () => {
      editParameter.addNewParameter();
    };

    const size = parameter ? 'xsmall' : 'small';
    const titleName = parameter ? t('edit_parameter.edit_title') : t('edit_parameter.create_title');
    const triggerButtonName = parameter ? t('common:actions.edit') : t('edit_parameter.create_title');

    const modal = (
      <BootstrapModalWrapper showModal={openModalKey === modalKey} onHide={closeModal} bsSize="large">
        <Modal.Header>
          <Modal.Title>{t('parameter_list.modal_title')}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ContentPackEditParameter
            ref={(node) => {
              editParameter = node;
            }}
            parameters={contentPack.parameters}
            onUpdateParameter={(newParameter) => {
              onAddParameter(newParameter, parameter);
              closeModal();
            }}
            parameterToEdit={parameter}
          />
        </Modal.Body>
        <Modal.Footer>
          <ModalSubmit onSubmit={addParameter} onCancel={closeModal} submitButtonText={titleName} />
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    return (
      <>
        <Button bsStyle="info" bsSize={size} title={t('parameter_list.edit_modal')} onClick={openModal}>
          {triggerButtonName}
        </Button>
        {modal}
      </>
    );
  }

  render() {
    const { readOnly } = this.props;
    const { filteredParameters } = this.state;

    const headers = readOnly
      ? [
          t('parameter_list.title'),
          t('parameter_list.name'),
          t('parameter_list.description'),
          t('parameter_list.value_type'),
          t('parameter_list.default_value'),
          t('parameter_list.used'),
        ]
      : [
          t('parameter_list.title'),
          t('parameter_list.name'),
          t('parameter_list.description'),
          t('parameter_list.value_type'),
          t('parameter_list.default_value'),
          t('parameter_list.used'),
          t('parameter_list.action'),
        ];

    return (
      <div>
        <h2>{t('parameter_list.list_title')}</h2>
        <br />
        {!readOnly && this._parameterModal()}
        {!readOnly && (
          <span>
            <br />
            <br />
          </span>
        )}
        <SearchForm
          onSearch={this._filterParameters}
          onReset={() => {
            this._filterParameters('');
          }}
        />
        <DataTable
          id="parameter-list"
          headers={headers}
          className={ContentPackParameterListStyle.scrollable}
          sortByKey="title"
          noDataText={t('parameter_list.no_data')}
          filterKeys={[]}
          rows={filteredParameters}
          dataRowFormatter={this._parameterRowFormatter}
        />
      </div>
    );
  }
}

export default ContentPackParameterList;
