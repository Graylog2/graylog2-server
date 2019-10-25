Using `Wizard` as uncontrolled component:
```js
import createReactClass from 'create-react-class';

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

Using `Wizard` as controlled component with no previous/next buttons and no preview:
```js
import createReactClass from 'create-react-class';

const Component1 = createReactClass({

  render() {
    return (<span>
      Type 'hello': <input value={this.props.input_value} onChange={this.props.onChange} />
    </span>);
  },
});

const ControlledWizardExample = createReactClass({
  getInitialState() {
    return {
      activeStep: 'Key3',
      input_value: "",
    };
  },

  onChange(e) {
    this.setState({ input_value: e.target.value });
  },

  changeStep(nextStep) {
    if (nextStep === 'Key2') {
      this.setState({ activeStep: 'Key3' });
      return;
    }

    this.setState({ activeStep: nextStep });
  },

  render() {
    const steps = [
      { key: 'Key1', title: 'Title1', component: (<Component1 input_value={this.state.input_value} onChange={this.onChange}/>) },
      { key: 'Key2', title: 'Title2', component: (<div>Component2</div>), disabled: false },
      { key: 'Key3', title: 'Title3', component: (<div style={{ backgroundColor: 'lightblue' }}>Component3</div>), disabled: false },
      { key: 'Key4', title: 'Title4', component: (<div>Component4</div>), disabled: false },
    ];

    return (
      <Wizard activeStep={this.state.activeStep} steps={steps} horizontal={this.props.horizontal} onStepChange={this.changeStep} hidePreviousNextButtons />
    );
  },
});

<div>
    <p>Goes to <em>Title3</em> when selecting <em>Title2</em>.</p>
    <ControlledWizardExample horizontal />
</div>
```
