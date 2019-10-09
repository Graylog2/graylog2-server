// @flow strict
import * as React from 'react';
import uuid from 'uuid/v4';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DropdownButton, MenuItem } from 'components/graylog';
import { ViewStore } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';

const menuTitle = <React.Fragment><i className="fa fa-plus" />{' '}Create</React.Fragment>;

type Props = {};

type State = {
  overflowingComponents: { [string]: React.Node },
};

export type CreatorProps = {
  view: View,
};
type CreatorType = 'preset' | 'generic';
type CreatorFunction = (CreatorProps) => ?React.Node;

type FunctionalCreator = {|
  func: CreatorFunction,
  title: string,
  type: CreatorType,
  condition?: () => boolean,
|};

type CreatorComponentProps = {
  onClose: () => void,
};

type ComponentCreator = {|
  component: React.ComponentType<CreatorComponentProps>,
  title: string,
  type: CreatorType,
  condition?: () => boolean,
|};

type Creator = ComponentCreator | FunctionalCreator;

class AddWidgetButton extends React.Component<Props, State> {
  state = {
    overflowingComponents: {},
  };

  _createHandlerFor = (creator: Creator): CreatorFunction => {
    const { view } = ViewStore.getInitialState();
    if (creator.func) {
      return () => creator.func({ view });
    }
    if (creator.component) {
      const CreatorComponent = creator.component;
      return () => {
        const id = uuid();
        const onClose = () => this.setState((state) => {
          const { overflowingComponents } = state;
          delete overflowingComponents[id];
          return { overflowingComponents };
        });
        const renderedComponent = <CreatorComponent key={creator.title} onClose={onClose} />;
        this.setState((state) => {
          const { overflowingComponents } = state;
          overflowingComponents[id] = renderedComponent;
          return { overflowingComponents };
        });
      };
    }
    throw new Error(`Invalid binding for creator: ${JSON.stringify(creator)} - has neither 'func' nor 'component'.`);
  };

  _createMenuItem = (creator: Creator): React.Node => (
    <MenuItem key={creator.title}
              onSelect={this._createHandlerFor(creator)}
              disabled={creator.condition ? !creator.condition() : false}>
      {creator.title}
    </MenuItem>
  );

  render() {
    const creators: Array<Creator> = PluginStore.exports('creators');
    const presets = creators.filter(c => (c.type === 'preset'))
      .map(this._createMenuItem);
    const generic = creators.filter(c => (c.type === 'generic'))
      .map(this._createMenuItem);
    const { overflowingComponents } = this.state;
    // $FlowFixMe: Object.value signature is in the way
    const components: Array<React.Node> = Object.values(overflowingComponents);
    return (
      <React.Fragment>
        <DropdownButton title={menuTitle} id="add-widget-button-dropdown" bsStyle="info" pullRight>
          {presets}
          <MenuItem divider />
          {generic}
        </DropdownButton>
        {components}
      </React.Fragment>
    );
  }
}

export default AddWidgetButton;
