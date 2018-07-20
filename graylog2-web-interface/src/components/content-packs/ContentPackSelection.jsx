import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { ExpandableList, ExpandableListItem, SearchForm } from 'components/common';
import FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';

class ContentPackSelection extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onStateChange: PropTypes.func,
    entities: PropTypes.object,
    selectedEntities: PropTypes.object,
  };

  static defaultProps = {
    onStateChange: () => {},
    entities: {},
    selectedEntities: {},
  };

  static _toDisplayTitle(title) {
    const newTitle = title.split('_').join(' ');
    return newTitle[0].toUpperCase() + newTitle.substr(1);
  }

  constructor(props) {
    super(props);
    this._bindValue = this._bindValue.bind(this);
    this.state = {
      contentPack: ObjectUtils.clone(this.props.contentPack),
      filteredEntities: ObjectUtils.clone(this.props.entities),
      filter: '',
      isFiltered: false,
    };
  }

  componentWillReceiveProps(nextProps) {
    this.setState({ filteredEntities: nextProps.entities, contentPack: nextProps.contentPack });
    if (this.state.isFiltered) {
      this._filterEntities(this.state.filter);
    }
  }

  _updateField(name, value) {
    const updatedPack = ObjectUtils.clone(this.state.contentPack);
    updatedPack[name] = value;
    this.props.onStateChange({ contentPack: updatedPack });
    this.setState({ contentPack: updatedPack });
  }

  _bindValue(event) {
    this._updateField(event.target.name, FormsUtils.getValueFromInput(event.target));
  }

  _updateSelectionEntity = (entity) => {
    const newSelection = ObjectUtils.clone(this.props.selectedEntities);
    newSelection[entity.type] = (newSelection[entity.type] || []);
    const index = newSelection[entity.type].findIndex((e) => { return e.id === entity.id; });
    if (index < 0) {
      newSelection[entity.type].push(entity);
    } else {
      newSelection[entity.type].splice(index, 1);
    }
    this.props.onStateChange({ selectedEntities: newSelection });
  };

  _updateSelectionGroup = (type) => {
    const newSelection = ObjectUtils.clone(this.props.selectedEntities);
    if (this._isGroupSelected(type)) {
      newSelection[type] = [];
    } else {
      newSelection[type] = this.props.entities[type];
    }

    this.props.onStateChange({ selectedEntities: newSelection });
  };

  _isUndetermined(type) {
    if (!this.props.selectedEntities[type]) {
      return false;
    }

    return !(this.props.selectedEntities[type].length === this.props.entities[type].length ||
       this.props.selectedEntities[type].length === 0);
  }

  _isSelected(entity) {
    if (!this.props.selectedEntities[entity.type]) {
      return false;
    }

    return this.props.selectedEntities[entity.type].findIndex((e) => { return e.id === entity.id; }) >= 0;
  }

  _isGroupSelected(type) {
    if (!this.props.selectedEntities[type]) {
      return false;
    }
    return this.props.selectedEntities[type].length === this.props.entities[type].length;
  }

  _onSetFilter = (filter) => {
    this._filterEntities(filter);
  };

  _onClearFilter = () => {
    this._filterEntities('');
  };

  _filterEntities = (filterArg) => {
    const filter = filterArg;
    if (filter.length <= 0) {
      this.setState({ filteredEntities: ObjectUtils.clone(this.props.entities), isFiltered: false, filter: filter });
      return;
    }
    const filtered = Object.keys(this.props.entities).reduce((result, type) => {
      const filteredEntities = ObjectUtils.clone(result);
      filteredEntities[type] = this.props.entities[type].filter((entity) => {
        const regexp = RegExp(filter, 'i');
        return regexp.test(entity.title);
      });
      return filteredEntities;
    }, {});
    this.setState({ filteredEntities: filtered, isFiltered: true, filter: filter });
  };

  render() {
    const entitiesComponent = Object.keys(this.state.filteredEntities || {}).map((entityType) => {
      const group = this.state.filteredEntities[entityType];
      const entities = group.map((entity) => {
        const checked = this._isSelected(entity);
        return (<ExpandableListItem onChange={() => this._updateSelectionEntity(entity)}
                                    key={entity.id}
                                    checked={checked}
                                    expandable={false}
                                    header={entity.title} />);
      });
      if (group.length <= 0) {
        return null;
      }
      return (
        <ExpandableListItem key={entityType}
                            onChange={() => this._updateSelectionGroup(entityType)}
                            indetermined={this._isUndetermined(entityType)}
                            checked={this._isGroupSelected(entityType)}
                            stayExpanded={this.state.isFiltered}
                            expanded={this.state.isFiltered}
                            header={ContentPackSelection._toDisplayTitle(entityType)}>
          <ExpandableList>
            {entities}
          </ExpandableList>
        </ExpandableListItem>
      );
    });

    return (
      <div>
        <Row>
          <Col smOffset={1} lg={8}>
            <h2>General Information</h2>
            <br />
            <form className="content-selection-form" id="content-selection-form" onSubmit={(e) => { e.preventDefault(); }}>
              <fieldset>
                <Input name="name"
                       id="name"
                       type="text"
                       maxLength={250}
                       value={this.state.contentPack.name}
                       onChange={this._bindValue}
                       label="Name"
                       help="Give a descriptive name for this content pack."
                       required />
                <Input name="summary"
                       id="summary"
                       type="text"
                       maxLength={250}
                       value={this.state.contentPack.summary}
                       onChange={this._bindValue}
                       label="Summary"
                       help="Give a short summary of the content pack."
                       required />
                <Input name="description"
                       id="description"
                       type="textarea"
                       value={this.state.contentPack.description}
                       onChange={this._bindValue}
                       rows={6}
                       label="Description"
                       help="Give a long description of the content pack in markdown."
                       required />
                <Input name="vendor"
                       id="vendor"
                       type="text"
                       maxLength={250}
                       value={this.state.contentPack.vendor}
                       onChange={this._bindValue}
                       label="Vendor"
                       help="Who did this content pack and how can he be reached. e.g Name and eMail"
                       required />
                <Input name="url"
                       id="url"
                       type="text"
                       maxLength={250}
                       value={this.state.contentPack.url}
                       onChange={this._bindValue}
                       label="URL"
                       help="Where can I find the content pack. e.g. github url"
                       required />
              </fieldset>
            </form>
          </Col>
        </Row>
        <Row>
          <Col smOffset={1}>
            <h2>Content Pack selection</h2>
          </Col>
        </Row>
        <Row>
          <Col smOffset={1}>
            <SearchForm
              id="filter-input"
              onSearch={this._onSetFilter}
              onReset={this._onClearFilter}
              searchButtonLabel="Filter"
            />
          </Col>
        </Row>
        <Row>
          <Col smOffset={1} sm={8}>
            <ExpandableList>
              {entitiesComponent}
            </ExpandableList>
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackSelection;
