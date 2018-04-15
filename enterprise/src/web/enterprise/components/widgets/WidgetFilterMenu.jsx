import React from 'react';
import PropTypes from 'prop-types';
import { OverlayTrigger, Popover } from 'react-bootstrap';

import connect from 'stores/connect';

import QueryInput from '../searchbar/QueryInput';
import SearchStore from '../../stores/SearchStore';

import style from './WidgetFilterMenu.css';

class WidgetFilterMenu extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    onChange: PropTypes.func.isRequired,
  };
  constructor(props, context) {
    super(props, context);
    this.state = {
      filter: props.value,
    };
  }
  _onUpdate = () => {
    this.props.onChange(this.state.filter);
    this.overlayTrigger.hide();
  };
  render() {
    const popoverBottom = (
      <Popover id="popover-positioned-bottom" title="Widget Filter">
        <div className={style.flavorText}>
          You can limit the results used by this widget by adding a custom filter here.
        </div>
        <div className={style.filterInput}>
          <QueryInput onChange={value => this.setState({ filter: value })}
                      onExecute={this._onUpdate}
                      placeholder="Add new widget filter"
                      value={this.state.filter} />
        </div>
      </Popover>
    );
    return (
      <OverlayTrigger ref={(elem) => { this.overlayTrigger = elem; }} trigger="click" placement="bottom" overlay={popoverBottom} rootClose>
        {this.props.children}
      </OverlayTrigger>
    );
  }
};

export default connect(WidgetFilterMenu, { search: SearchStore });
