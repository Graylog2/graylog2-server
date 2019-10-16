Regular `ReactGridContainer`:
```js
import createReactClass from 'create-react-class';

const ReactGridContainerExample = createReactClass({
  getInitialState() {
    return {
      positions: {
        '1': { col: 0, row: 0, height: 1, width: 2 },
        '2': { col: 2, row: 0, height: 1, width: 4 },
        '3': { col: 6, row: 0, height: 1, width: 2 },
        '4': { col: 8, row: 0, height: 1, width: 4 },
      },
    };
  },

  onPositionsChange(nextPositions) {
    console.log('positions changed to ', nextPositions);
  },

  widgetDiv(id) {
    return (
      <div key={id} style={{ backgroundColor: '#f9f9f9', fontSize: '6em', textAlign: 'center' }}>
        {id}
      </div>
    );
  },

  render() {
    const { positions } = this.state;
    const columns = {
      xxl: 6,
      xl: 5,
      lg: 4,
      md: 4,
      sm: 4,
      xs: 4,
    };

    return (
      <ReactGridContainer positions={positions}
                          onPositionsChange={this.onPositionsChange}
                          columns={columns}>
        {this.widgetDiv(1)}
        {this.widgetDiv(2)}
        {this.widgetDiv(3)}
        {this.widgetDiv(4)}
      </ReactGridContainer>
    );
  },
});

<ReactGridContainerExample />
```


Lock or block resizing in `ReactGridContainer`:
```js
import createReactClass from 'create-react-class';
import { Button, ButtonToolbar } from 'components/graylog';

const ReactGridContainerExampleLocked = createReactClass({
  getInitialState() {
    return {
      positions: {
        '1': { col: 0, row: 0, height: 1, width: 2 },
        '2': { col: 2, row: 0, height: 1, width: 4 },
        '3': { col: 6, row: 0, height: 1, width: 2 },
        '4': { col: 8, row: 0, height: 1, width: 4 },
      },
      locked: false,
      isResizable: false,
    };
  },

  onPositionsChange(nextPositions) {
    console.log('positions changed to ', nextPositions);
  },

  toggleLocked() {
    this.setState({ locked: !this.state.locked });
  },

  toggleIsResizable() {
    this.setState({ isResizable: !this.state.isResizable });
  },

  widgetDiv(id) {
    return (
      <div key={id} style={{ backgroundColor: '#f9f9f9', fontSize: '6em', textAlign: 'center' }}>
        {id}
      </div>
    );
  },

  render() {
    const { positions, locked, isResizable } = this.state;
    const columns = {
      xxl: 6,
      xl: 5,
      lg: 4,
      md: 4,
      sm: 4,
      xs: 4,
    };

    return (
      <div>
        <ButtonToolbar>
          <Button bsStyle="info" active={locked} onClick={this.toggleLocked}>
            Locked is {String(locked)}
          </Button>
          <Button bsStyle="info" active={isResizable} onClick={this.toggleIsResizable}>
            isResizable is {String(isResizable)}
          </Button>
        </ButtonToolbar>
        <br/>
        <ReactGridContainer positions={positions}
                            onPositionsChange={this.onPositionsChange}
                            columns={columns}
                            rowHeight={100}
                            locked={locked}
                            isResizable={isResizable}>
          {this.widgetDiv(1)}
          {this.widgetDiv(2)}
          {this.widgetDiv(3)}
          {this.widgetDiv(4)}
        </ReactGridContainer>
      </div>
    );
  },
});

<ReactGridContainerExampleLocked />
```
