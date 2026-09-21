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

import { Icon, SearchForm } from 'components/common';
import { Col, HelpBlock, Row, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import { hasAcceptedProtocol } from 'util/URLUtils';
import InputDescription from 'components/common/InputDescription';
import ContentPackSelectionList from 'components/content-packs/ContentPackSelectionList';
import {
  defaultEntity,
  entityForSource,
  isEntityOfRow,
  rowEntityIds,
  type CatalogEntity,
  type EntitySource,
} from 'logic/content-packs/EntityCatalog';

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

  /*
   * The selection holds the entities themselves, which are deep structures, so only the array of the type being
   * touched is copied. Cloning the whole selection here made every click on a large pack noticeably slow.
   */
  _updateSelectionEntity = (entity) => {
    const { selectedEntities, onStateChange } = this.props;
    const typeName = entity.type.name;
    const current = selectedEntities[typeName] ?? [];
    const index = current.findIndex((e) => isEntityOfRow(entity, e));
    const newSelection = { ...selectedEntities };

    newSelection[typeName] =
      index < 0 ? [...current, defaultEntity(entity)] : [...current.slice(0, index), ...current.slice(index + 1)];

    this._handleTouched('selection');
    this._validate(newSelection);
    onStateChange({ selectedEntities: newSelection });
  };

  _updateSelectionGroup = (type: string) => {
    const { selectedEntities, entities, onStateChange } = this.props;
    const { isFiltered, filteredEntities } = this.state;

    const newSelection = { ...selectedEntities };

    if (isFiltered) {
      const selectedIds = new Set((newSelection[type] ?? []).map((entity) => entity.id));
      const newlySelected = filteredEntities[type]
        .filter((entity: CatalogEntity) => !rowEntityIds(entity).some((id) => selectedIds.has(id)))
        .map((entity) => defaultEntity(entity));

      newSelection[type] = [...(newSelection[type] ?? []), ...newlySelected];
    } else if (this._isGroupSelected(type)) {
      newSelection[type] = [];
    } else {
      newSelection[type] = entities[type].map((entity) => defaultEntity(entity));
    }

    this._handleTouched('selection');
    this._validate(newSelection);
    onStateChange({ selectedEntities: newSelection });
  };

  /* Switches which copy of a single entity gets exported. */
  _updateEntitySource = (entity, source: EntitySource) => {
    const { selectedEntities, onStateChange } = this.props;
    const typeName = entity.type.name;
    const replacement = entityForSource(entity, source);

    if (!replacement) {
      return;
    }

    const current = selectedEntities[typeName] ?? [];
    const isSelected = current.some((selected) => isEntityOfRow(entity, selected));
    const newSelection = { ...selectedEntities };

    /* Picking a source for an entity that is not checked yet selects it, so the control never needs two clicks. */
    newSelection[typeName] = isSelected
      ? current.map((selected) => (isEntityOfRow(entity, selected) ? replacement : selected))
      : [...current, replacement];

    this._handleTouched('selection');
    this._validate(newSelection);
    onStateChange({ selectedEntities: newSelection });
  };

  /*
   * Switches which copy of each selected entity in a group gets exported. Entities that only exist in one place
   * are left alone, so acting on a group that has nothing to choose from is a no-op.
   */
  _updateGroupSource = (type: string, source: EntitySource) => {
    const { selectedEntities, onStateChange } = this.props;
    const { filteredEntities } = this.state;

    /* Keyed by both of a row's ids, since the selection may hold either copy. */
    const catalogEntities: Map<string, CatalogEntity> = new Map(
      (filteredEntities[type] ?? []).flatMap((entity: CatalogEntity) =>
        rowEntityIds(entity).map((id) => [id, entity] as [string, CatalogEntity]),
      ),
    );
    const newSelection = { ...selectedEntities };

    newSelection[type] = (newSelection[type] ?? []).map((selected) => {
      const replacement = entityForSource(catalogEntities.get(selected.id), source);

      return replacement ?? selected;
    });

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
      this.setState({ filteredEntities: entities, isFiltered: false, filter: filter });

      return;
    }

    const filtered = Object.fromEntries(
      Object.keys(entities).map((type) => [
        type,
        entities[type].filter((entity) => {
          const regexp = RegExp(filter, 'i');

          return regexp.test(entity.title);
        }),
      ]),
    );

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
                For each entity you can export the currently installed instance (<Icon name="database" />) or the
                instance from the content pack revision you picked when creating this new version (
                <Icon name="package_2" />
                ). Use the buttons on an entity to choose which one, or the buttons on an entity type to change every
                entity checked in it at once.
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
              updateGroupSource={edit ? this._updateGroupSource : undefined}
              updateEntitySource={edit ? this._updateEntitySource : undefined}
            />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackSelection;
