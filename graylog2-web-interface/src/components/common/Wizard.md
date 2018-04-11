```js
const createReactClass = require('create-react-class');

const Component1 = createReactClass({

  render() {
    return (<span>
      Type 'hello': <input value={this.props.input_value} onChange={this.props.onChange} />
    </span>);
  },
});

const WizardExample = createReactClass({
  getInitialState() {
    return {
      input_value: "",
    };
  },

  onChange(e) {
    this.setState({ input_value: e.target.value });
  },

  enableNext() {
    return this.state.input_value !== 'hello';
  },

  render() {
    const steps = [
      { key: 'Key1', title: 'Title1', component: (<Component1 input_value={this.state.input_value} onChange={this.onChange}/>) },
      { key: 'Key2', title: 'Title2', component: (<div>Component2</div>), disabled: this.enableNext() },
      { key: 'Key3', title: 'Title3', component: (<div>Component3</div>), disabled: this.enableNext() },
    ];
 
    return (
      <Wizard steps={steps} horizontal={this.props.horizontal}>
        <div>Preview: {this.state.input_value}</div>
      </Wizard>
    );
  },
});

<div>
    <WizardExample horizontal={false} />
    <hr/>
    <WizardExample horizontal />
</div>

```