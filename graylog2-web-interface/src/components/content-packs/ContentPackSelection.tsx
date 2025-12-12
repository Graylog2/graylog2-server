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
import * as React from 'react';
import cloneDeep from 'lodash/cloneDeep';

import { Icon, SearchForm } from 'components/common';
import { Col, HelpBlock, Row, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import { hasAcceptedProtocol } from 'util/URLUtils';
import InputDescription from 'components/common/InputDescription';
import ContentPackSelectionList from 'components/content-packs/ContentPackSelectionList';

import style from './ContentPackSelection.css';

type ContentPackSelectionProps = {
  contentPack: any;
  onStateChange?: (...args: any[]) => void;
  entities?: any;
  selectedEntities?: any;
  edit?: boolean;
};

class ContentPackSelection extends React.Component<
  ContentPackSelectionProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    edit: false,
    onStateChange: () => {},
    entities: {},
    selectedEntities: {},
  };

  constructor(props) {
    super(props);

    const { entities, contentPack } = this.props;

    this.state = {
      contentPack: contentPack,
      filteredEntities: entities,
      filter: '',
      isFiltered: false,
      errors: {},
      touched: {},
    };
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState({ filteredEntities: nextProps.entities, contentPack: nextProps.contentPack });
    const { filter, isFiltered } = this.state;

    if (isFiltered) {
      this._filterEntities(filter);
    }
  }

  _updateField = (name, value) => {
    const { contentPack } = this.state;
    const { onStateChange } = this.props;
    const updatedPack = contentPack.toBuilder()[name](value).build();

    onStateChange({ contentPack: updatedPack });
    this.setState({ contentPack: updatedPack }, this._validate);
  };

  _validate = (newSelection?) => {
    const mandatoryFields = ['name', 'summary', 'vendor'];
    const { contentPack } = this.state;
    const { selectedEntities: prevSelectedEntities } = this.props;
    const selectedEntities = newSelection || prevSelectedEntities;

    const errors: { [key: string]: string } = mandatoryFields.reduce((acc, field) => {
      const newErrors = acc;

      if (!contentPack[field] || contentPack[field].length <= 0) {
        newErrors[field] = 'Must be filled out.';
      }

      return newErrors;
    }, {});

    if (contentPack.url) {
      try {
        if (!hasAcceptedProtocol(contentPack.url)) {
          errors.url = 'Must use a URL starting with http or https.';
        }
      } catch (_e) {
        errors.url = 'Invalid URL';
      }
    }

    const selectionEmpty =
      Object.keys(selectedEntities).reduce((acc, entityGroup) => acc + selectedEntities[entityGroup].length, 0) <= 0;

    if (selectionEmpty) {
      errors.selection = 'Select at least one entity.';
    }

    this.setState({ errors });
  };

  _bindValue = (event) => {
    this._updateField(event.target.name, getValueFromInput(event.target));
  };

  _handleTouched = (name) => {
    this.setState(
      (prevState) => ({
        touched: {
          ...prevState.touched,
          [name]: true,
          selection: true,
        },
      }),
      this._validate,
    );
  };

  _error = (name) => (this.state.touched[name] ? this.state.errors[name] : undefined);

  _updateSelectionEntity = (entity) => {
    const { selectedEntities, onStateChange } = this.props;
    const typeName = entity.type.name;
    const newSelection = cloneDeep(selectedEntities);

    newSelection[typeName] = newSelection[typeName] || [];
    const index = newSelection[typeName].findIndex((e) => e.id === entity.id);

    if (index < 0) {
      newSelection[typeName].push(entity);
    } else {
      newSelection[typeName].splice(index, 1);
    }

    this._handleTouched('selection');
    this._validate(newSelection);
    onStateChange({ selectedEntities: newSelection });
  };

  _updateSelectionGroup = (type: string) => {
    const { selectedEntities, entities, onStateChange } = this.props;
    const { isFiltered, filteredEntities } = this.state;

    const newSelection = cloneDeep(selectedEntities);

    if (isFiltered) {
      if (newSelection[type]) {
        newSelection[type] = [...newSelection[type], ...filteredEntities[type]];
      } else {
        newSelection[type] = filteredEntities[type];
      }
    } else if (this._isGroupSelected(type)) {
      newSelection[type] = [];
    } else {
      newSelection[type] = entities[type];
    }

    this._handleTouched('selection');
    this._validate(newSelection);
    onStateChange({ selectedEntities: newSelection });
  };

  _isGroupSelected = (type) => {
    const { selectedEntities, entities } = this.props;

    if (!selectedEntities[type]) {
      return false;
    }

    return selectedEntities[type].length === entities[type].length;
  };

  _onSetFilter = (filter) => {
    this._filterEntities(filter);
  };

  _onClearFilter = () => {
    this._filterEntities('');
  };

  _filterEntities = (filterArg) => {
    const { entities } = this.props;
    const filter = filterArg;

    if (filter.length <= 0) {
      this.setState({ filteredEntities: cloneDeep(entities), isFiltered: false, filter: filter });

      return;
    }

    const filtered = Object.keys(entities).reduce((result, type) => {
      const filteredEntities = cloneDeep(result);

      filteredEntities[type] = entities[type].filter((entity) => {
        const regexp = RegExp(filter, 'i');

        return regexp.test(entity.title);
      });

      return filteredEntities;
    }, {});

    this.setState({ filteredEntities: filtered, isFiltered: true, filter: filter });
  };

  render() {
    const { filteredEntities = {}, errors, touched, isFiltered, contentPack } = this.state;
    const { edit, selectedEntities } = this.props;

    return (
      <div>
        <Row>
          <Col smOffset={1} lg={8}>
            <h2>General Information</h2>
            <br />
            <form
              className="content-selection-form"
              id="content-selection-form"
              onSubmit={(e) => {
                e.preventDefault();
              }}>
              <fieldset>
                <Input
                  name="name"
                  id="name"
                  type="text"
                  maxLength={250}
                  value={contentPack.name ?? ''}
                  onChange={this._bindValue}
                  onBlur={() => this._handleTouched('name')}
                  label="Name"
                  help="Required. Give a descriptive name for this content pack."
                  error={this._error('name')}
                  required
                />
                <Input
                  name="summary"
                  id="summary"
                  type="text"
                  maxLength={250}
                  value={contentPack.summary ?? ''}
                  onChange={this._bindValue}
                  onBlur={() => this._handleTouched('summary')}
                  label="Summary"
                  help="Required. Give a short summary of the content pack."
                  error={this._error('summary')}
                  required
                />
                <Input
                  name="description"
                  id="description"
                  type="textarea"
                  value={contentPack.description}
                  onChange={this._bindValue}
                  onBlur={() => this._handleTouched('description')}
                  rows={6}
                  label="Description"
                  help="Give a long description of the content pack in markdown."
                />
                <Input
                  name="vendor"
                  id="vendor"
                  type="text"
                  maxLength={250}
                  value={contentPack.vendor ?? ''}
                  onChange={this._bindValue}
                  onBlur={() => this._handleTouched('vendor')}
                  label="Vendor"
                  help="Required. Who did this content pack and how can they be reached, e.g. Name and email."
                  error={this._error('vendor')}
                  required
                />
                <Input
                  name="url"
                  id="url"
                  type="text"
                  maxLength={250}
                  value={contentPack.url}
                  onChange={this._bindValue}
                  onBlur={() => this._handleTouched('url')}
                  label="URL"
                  help="Where can I find the content pack. e.g. github url"
                  error={this._error('url')}
                />
              </fieldset>
            </form>
          </Col>
        </Row>
        <Row>
          <Col smOffset={1} lg={8}>
            <h2>Content Pack selection</h2>
            {edit && (
              <HelpBlock>
                You can select between installed entities from the server (<Icon name="dns" />) or entities from the
                former content pack revision (<Icon name="archive" className={style.contentPackEntity} />
                ).
              </HelpBlock>
            )}
          </Col>
        </Row>
        <Row>
          <Col smOffset={1} lg={8}>
            <SearchForm onSearch={this._onSetFilter} onReset={this._onClearFilter} />
          </Col>
        </Row>
        <Row>
          <Col smOffset={1} sm={8} lg={8}>
            {touched.selection && errors.selection && <InputDescription error={errors.selection} />}
            <ContentPackSelectionList
              entities={filteredEntities}
              selectedEntities={selectedEntities}
              isFiltered={isFiltered}
              isGroupSelected={this._isGroupSelected}
              updateSelectionEntity={this._updateSelectionEntity}
              updateSelectionGroup={this._updateSelectionGroup}
            />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackSelection;
