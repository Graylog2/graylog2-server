import PropTypes from 'prop-types';
import React from 'react';

import { AutoAffix } from 'react-overlays';
import { ScrollButton, Spinner, Wizard } from 'components/common';
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

  _stepChanged = (selectedStep) => {
    switch (selectedStep) {
      case 'parameters': {
        const { onStateChange, selectedEntities, fetchedEntities, onGetEntities, contentPack } = this.props;
        const newContentPack = contentPack.toBuilder()
          .entities(fetchedEntities || [])
          .build();
        onStateChange({ contentPack: newContentPack });
        if (Object.keys(selectedEntities).length > 0) {
          onGetEntities(selectedEntities);
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

  _disableParameters() {
    const { contentPack, selectedEntities } = this.props;
    const content = contentPack;
    const selection = Object.keys(selectedEntities)
      .reduce((acc, key) => { return acc + selectedEntities[key].length; }, 0) > 0;
    return !(content.name && content.summary && content.vendor && selection);
  }

  _disablePreview() {
    const { selectedStep } = this.state;
    return selectedStep === 'selection' || !selectedStep;
  }

  _prepareForPreview() {
    const { onStateChange, fetchedEntities, contentPack, appliedParameter } = this.props;
    const newEntities = fetchedEntities.map((entity) => {
      const parameters = appliedParameter[entity.id] || [];
      const newEntityBuilder = entity.toBuilder();
      const entityData = new ValueReferenceData(entity.data);
      const configPaths = entityData.getPaths();

      Object.keys(configPaths).forEach((path) => {
        const index = parameters.findIndex((paramMap) => { return paramMap.configKey === path; });
        if (index >= 0) {
          configPaths[path].setParameter(parameters[index].paramName);
        }
      });
      newEntityBuilder.data(entityData.getData()).parameters(contentPack.parameters);
      return newEntityBuilder.build();
    });
    const newContentPack = contentPack.toBuilder()
      .entities(newEntities)
      .build();

    onStateChange({ contentPack: newContentPack });
  }

  render() {
    const { edit, entityIndex, appliedParameter, selectedEntities, contentPack, onSave, onStateChange } = this.props;
    const { selectedStep } = this.state;

    if (!contentPack) {
      return (<Spinner />);
    }

    const selectionComponent = (
      <ContentPackSelection contentPack={contentPack}
                            selectedEntities={selectedEntities}
                            edit={edit}
                            onStateChange={onStateChange}
                            entities={entityIndex} />
    );
    const parameterComponent = (
      <ContentPackParameters contentPack={contentPack}
                             onStateChange={onStateChange}
                             appliedParameter={appliedParameter} />
    );
    const previewComponent = (
      <ContentPackPreview contentPack={contentPack}
                          onSave={onSave} />
    );
    const steps = [
      { key: 'selection', title: 'Content Selection', component: selectionComponent },
      { key: 'parameters', title: 'Parameters', component: parameterComponent, disabled: this._disableParameters() },
      { key: 'preview', title: 'Preview', component: previewComponent, disabled: this._disablePreview() },
    ];

    return (
      <div>
        <Wizard steps={steps} onStepChange={this._stepChanged} affixed>
          {selectedStep !== 'preview' ? (
            <AutoAffix viewportOffsetTop={65}>
              <div>
                <ContentPackDetails contentPack={contentPack} />
              </div>
            </AutoAffix>
          ) : undefined}
        </Wizard>
        <ScrollButton possition="middle" />
      </div>
    );
  }
}

export default ContentPackEdit;
