```js
import createReactClass from 'create-react-class';
import { Button } from 'components/graylog';

const BootstrapModalConfirmExample = createReactClass({
  getInitialState() {
    return {
      confirmed: undefined,
    };
  },

  openConfirmation() {
    this.modal.open();
  },

  onCancel() {
    this.setState({ confirmed: false });
  },

  onConfirm(callback) {
    this.setState({ confirmed: true });
    callback();
  },

  render() {
    const { confirmed } = this.state;
    return (
      <div>
        <p className={confirmed ? 'bg-success' : 'bg-danger'}>
          {confirmed === undefined ? 'You did not open the confirmation yet' : confirmed ? 'You confirmed the action' : 'You did not confirm the action' }
        </p>
        <Button onClick={this.openConfirmation}>Open confirmation</Button>
        <BootstrapModalConfirm ref={(c) => { this.modal = c; }}
                               title="Confirm this"
                               onConfirm={this.onConfirm}
                               onCancel={this.onCancel}>
           Are you sure you want to do this?
        </BootstrapModalConfirm>
      </div>
    );
  }
});

<BootstrapModalConfirmExample />
```
