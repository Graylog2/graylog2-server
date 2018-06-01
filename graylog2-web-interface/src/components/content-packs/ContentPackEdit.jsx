import PropTypes from 'prop-types';
import React from 'react';

import { AutoAffix } from 'react-overlays';
import { Spinner, Wizard, ScrollButton } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';

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
  };

  static defaultProps = {
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
    const selection = Object.keys(this.props.selectedEntities).length !== 0;
    return !(content.name && content.summary && content.description && content.vendor &&
      selection);
  }

  _disablePreview() {
    return this.state.selectedStep === 'selection' || !this.state.selectedStep;
  }

  _prepareForPreview() {
    const typeRegExp = RegExp(/\.type$/);
    const newContentPack = ObjectUtils.clone(this.props.contentPack);
    const entities = ObjectUtils.clone(this.props.fetchedEntities);
    newContentPack.entities = entities.map((entity) => {
      const parameters = this.props.appliedParameter[entity.id] || [];
      const newEntity = ObjectUtils.clone(entity);
      const entityData = newEntity.data;
      const configKeys = ObjectUtils.getPaths(entityData)
        .filter(configKey => typeRegExp.test(configKey))
        .map((configKey) => { return configKey.replace(typeRegExp, ''); });
      configKeys.forEach((path) => {
        const index = parameters.findIndex((paramMap) => { return paramMap.configKey === path; });
        let newValue;
        if (index >= 0) {
          newValue = { type: 'parameter', value: parameters[index].paramName };
        } else {
          newValue = ObjectUtils.getValue(entityData, path);
        }
        ObjectUtils.setValue(entityData, path, newValue);
      });
      newEntity.data = entityData;
      return newEntity;
    });

    this.props.onStateChange({ contentPack: newContentPack });
  }

  _stepChanged = (selectedStep) => {
    switch (selectedStep) {
      case 'parameters': {
        const newContentPack = ObjectUtils.clone(this.props.contentPack);
        newContentPack.entities = this.props.fetchedEntities || [];
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
                            onStateChange={this.props.onStateChange}
                            entities={this.props.entityIndex} />);
    const parameterComponent = (
      <ContentPackParameters contentPack={this.props.contentPack}
                             onStateChange={this.props.onStateChange}
                             appliedParameter={this.props.appliedParameter} />);
    const previewComponent = (
      <ContentPackPreview contentPack={this.props.contentPack}
                          onSave={this.props.onSave} />);
    const steps = [
      { key: 'selection', title: 'Content Selection', component: selectionComponent },
      { key: 'parameters', title: 'Parameters', component: parameterComponent, disabled: this._disableParameters() },
      { key: 'preview', title: 'Preview', component: previewComponent, disabled: this._disablePreview() },
    ];

    return (
      <div>
        <Wizard steps={steps} onStepChange={this._stepChanged} affixed>
          {this.state.selectedStep !== 'preview' ? <AutoAffix viewportOffsetTop={65}>
            <div>
              <ContentPackDetails contentPack={this.props.contentPack} />
            </div>
          </AutoAffix> : undefined}
        </Wizard>
        <ScrollButton possition="middle" />
      </div>
    );
  }
}

export default ContentPackEdit;
