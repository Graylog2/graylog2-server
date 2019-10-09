// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { CurrentViewType } from 'views/components/CustomPropTypes';
import connect from 'stores/connect';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import Value from 'views/components/Value';
import { ViewStore } from 'views/stores/ViewStore';
import DecoratedValue from 'views/components/messagelist/decoration/DecoratedValue';
import CustomHighlighting from 'views/components/messagelist/CustomHighlighting';
import style from './NumberVisualization.css';

type Props = {
  data: Rows,
  width: number,
  height: number,
  fields: FieldTypeMappingsList,
  currentView: CurrentViewType,
};

type State = {
  fontSize: number,
};

class NumberVisualization extends React.Component<Props, State> {
  static propTypes = {
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    currentView: PropTypes.object.isRequired,
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    fields: PropTypes.object.isRequired,
  };

  container: HTMLElement | null;

  constructor(props) {
    super(props);
    this.state = {
      fontSize: 20,
    };
  }

  componentDidUpdate() {
    const container = this.getContainer();
    if (!container) {
      return;
    }

    const { fontSize } = this.state;
    const { width, height } = this.props;
    const { childNodes } = container;

    if (childNodes.length <= 0) {
      return;
    }

    const content = childNodes[0];
    // $FlowFixMe offsetWidth is part of Node!
    const contentWidth = content.offsetWidth;
    // $FlowFixMe offsetHeight is part of Node!
    const contentHeight = content.offsetHeight;

    const widthMultiplier = (width * 0.8) / contentWidth;
    const heightMultiplier = (height * 0.8) / contentHeight;
    const multiplier = Math.min(widthMultiplier, heightMultiplier);
    if (Math.abs(1 - multiplier) <= 0.01) {
      return;
    }

    const newFontsize = Math.floor(fontSize * multiplier);

    if (fontSize !== newFontsize) {
      // eslint-disable-next-line react/no-did-update-set-state
      this.setState({ fontSize: newFontsize });
    }
  }

  getContainer() {
    return this.container;
  }

  getContainerRef = (node) => {
    this.container = node;
  };

  _extractValueAndField = (data) => {
    if (!data || !data[0]) {
      return { value: undefined, field: undefined };
    }
    const results = data[0];
    if (results.source === 'leaf') {
      const leaf = results.values.find(f => f.source === 'row-leaf');
      if (leaf && leaf.source === 'row-leaf') {
        return { value: leaf.value, field: leaf.key[0] };
      }
    }
    return { value: undefined, field: undefined };
  };

  render() {
    const { fontSize } = this.state;
    const { currentView, fields, data } = this.props;
    const { activeQuery } = currentView;
    const { value, field } = this._extractValueAndField(data);
    if (!field || (value !== 0 && !value)) {
      return 'N/A';
    }

    return (
      <div ref={this.getContainerRef} className={style.container} style={{ fontSize: `${fontSize}px` }}>
        <CustomHighlighting field={field} value={value}>
          <Value field={field}
                 type={fieldTypeFor(field, fields)}
                 value={value}
                 queryId={activeQuery}
                 render={DecoratedValue} />
        </CustomHighlighting>
      </div>
    );
  }
}

const ConnectedNumberVisualization = connect(NumberVisualization, { currentView: ViewStore });
ConnectedNumberVisualization.type = 'numeric';

export default ConnectedNumberVisualization;
