import React from 'react';
import Reflux from 'reflux';
import { Button, ButtonGroup, DropdownButton } from 'react-bootstrap';

import RefreshStore from 'stores/tools/RefreshStore';

import RefreshActions from 'actions/tools/RefreshActions';

const RefreshControls = React.createClass({
  mixins: [Reflux.connect(RefreshStore, 'refresh')],
  render() {
    return (
      <ButtonGroup>
        <Button bsSize="small" onClick={() => this.state.refresh.enabled ? RefreshActions.disable() : RefreshActions.enable()}>
          {this.state.refresh.enabled ? <i className="fa fa-pause"/> : <i className="fa fa-play"/>}
        </Button>

        <DropdownButton bsSize="small" title={this.state.refresh.enabled ? 'Update every ' + (this.state.refresh.interval / 1000) + ' seconds' : 'Not updating'} id="refresh-options-dropdown">
          <div className="form-control" style={{'height': '100%', 'width': '150%', display: 'flex'}}>
            1s{' '}
            <input id="mySlider"
                   type="range"
                   value={this.state.refresh.interval}
                   min={1000}
                   max={60 * 1000}
                   onChange={(evt) => RefreshActions.changeInterval(evt.target.value)}
                   step={1000} />
            {' '}60s
          </div>
        </DropdownButton>
      </ButtonGroup>
    );
  },
});

export default RefreshControls;
