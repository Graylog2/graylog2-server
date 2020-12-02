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
import { AutoAffix } from 'react-overlays';

import { Spinner, Wizard, ScrollButton } from 'components/common';
import ValueReferenceData from 'util/ValueReferenceData';
import ContentPackSelection from 'components/content-packs/ContentPackSelection';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import ContentPackPreview from 'components/content-packs/ContentPackPreview';
import ContentPackParameters from 'components/content-packs/ContentPackParameters';

class ContentPackEdit extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object,
    onGetEntities: PropTypes.func,
    onStateChange: PropTypes.func,
    onSave: PropTypes.func,
    fetchedEntities: PropTypes.array,
    entityIndex: PropTypes.object,
    selectedEntities: PropTypes.object,
    appliedParameter: PropTypes.object,
    edit: PropTypes.bool,
  };

  static defaultProps = {
    edit: false,
    contentPack: undefined,
    onGetEntities: () => {},
    onStateChange: () => {},
    onSave: () => {},
    fetchedEntities: [],
    entityIndex: {},
    selectedEntities: {},
    appliedParameter: {},
  };

  constructor(props) {
    super(props);

    this.state = {
      selectedStep: 'selection',
    };
  }

  _disableParameters() {
    const content = this.props.contentPack;
    const { selectedEntities } = this.props;
    const selection = Object.keys(selectedEntities)
      .reduce((acc, key) => { return acc + selectedEntities[key].length; }, 0) > 0;

    return !(content.name && content.summary && content.vendor && selection);
  }

  _disablePreview() {
    return this.state.selectedStep === 'selection' || !this.state.selectedStep;
  }

  _prepareForPreview() {
    const newEntities = this.props.fetchedEntities.map((entity) => {
      const parameters = this.props.appliedParameter[entity.id] || [];
      const newEntityBuilder = entity.toBuilder();
      const entityData = new ValueReferenceData(entity.data);
      const configPaths = entityData.getPaths();

      Object.keys(configPaths).forEach((path) => {
        const index = parameters.findIndex((paramMap) => { return paramMap.configKey === path; });

        if (index >= 0) {
          configPaths[path].setParameter(parameters[index].paramName);
        }
      });

      newEntityBuilder.data(entityData.getData()).parameters(this.props.contentPack.parameters);

      return newEntityBuilder.build();
    });
    const newContentPack = this.props.contentPack.toBuilder()
      .entities(newEntities)
      .build();

    this.props.onStateChange({ contentPack: newContentPack });
  }

  _stepChanged = (selectedStep) => {
    switch (selectedStep) {
      case 'parameters': {
        const newContentPack = this.props.contentPack.toBuilder()
          .entities(this.props.fetchedEntities || [])
          .build();

        this.props.onStateChange({ contentPack: newContentPack });

        if (Object.keys(this.props.selectedEntities).length > 0) {
          this.props.onGetEntities(this.props.selectedEntities);
        }

        break;
      }

      case 'preview': {
        this._prepareForPreview();
        break;
      }

      default: {
        break;
      }
    }

    this.setState({ selectedStep: selectedStep });
  };

  render() {
    if (!this.props.contentPack) {
      return (<Spinner />);
    }

    const selectionComponent = (
      <ContentPackSelection contentPack={this.props.contentPack}
                            selectedEntities={this.props.selectedEntities}
                            edit={this.props.edit}
                            onStateChange={this.props.onStateChange}
                            entities={this.props.entityIndex} />
    );
    const parameterComponent = (
      <ContentPackParameters contentPack={this.props.contentPack}
                             onStateChange={this.props.onStateChange}
                             appliedParameter={this.props.appliedParameter} />
    );
    const previewComponent = (
      <ContentPackPreview contentPack={this.props.contentPack}
                          onSave={this.props.onSave} />
    );
    const steps = [
      { key: 'selection', title: 'Content Selection', component: selectionComponent },
      { key: 'parameters', title: 'Parameters', component: parameterComponent, disabled: this._disableParameters() },
      { key: 'preview', title: 'Preview', component: previewComponent, disabled: this._disablePreview() },
    ];

    return (
      <div>
        <Wizard steps={steps} onStepChange={this._stepChanged} affixed>
          {this.state.selectedStep !== 'preview' ? (
            <AutoAffix viewportOffsetTop={65}>
              <div>
                <ContentPackDetails contentPack={this.props.contentPack} />
              </div>
            </AutoAffix>
          ) : undefined}
        </Wizard>
        <ScrollButton position="middle" />
      </div>
    );
  }
}

export default ContentPackEdit;
