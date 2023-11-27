```js
import createReactClass from 'create-react-class';
import { Button } from 'components/bootstrap';

const BootstrapModalConfirmExample = createReactClass({
  getInitialState() {
    return {
      showModal: false,
      confirmed: undefined,
    };
  },

  openConfirmation() {
    this.setState({ showModal: true });
  },

  onCancel() {
    this.setState({ confirmed: false, showModal: false });
  },

  onConfirm(callback) {
    this.setState({ confirmed: true, showModal: false });
    callback();
  },

  render() {
    const { confirmed, showModal } = this.state;
    return (
      <div>
        <p className={confirmed ? 'bg-success' : 'bg-danger'}>
          {confirmed === undefined ? 'You did not open the confirmation yet' : confirmed ? 'You confirmed the action' : 'You did not confirm the action' }
        </p>
        <Button onClick={this.openConfirmation}>Open confirmation</Button>
        <BootstrapModalConfirm showModal={showModal}
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
