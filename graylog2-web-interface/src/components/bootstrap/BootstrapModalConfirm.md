```jsx
import { Button } from 'components/graylog';

const BootstrapModalConfirmExample = () => {
  const [confirmed, setConfirmed] = React.useState();
  const modalRef = React.useRef();
  
  const openConfirmation = () => {
    modalRef.current.open();
  }
  
  const onCancel = () => {
    setConfirmed(false);
  } 

  const onConfirm = (callback = () => {}) => {
    setConfirmed(true);
    callback();
  }

  const message = confirmed === undefined 
    ? 'You did not open the confirmation yet' 
    : confirmed 
      ? 'You confirmed the action' 
      : 'You did not confirm the action'

  return (
    <div>
      <p className={confirmed ? 'bg-success' : 'bg-danger'}>
        {message}
      </p>
      <Button onClick={openConfirmation}>Open confirmation</Button>
      <BootstrapModalConfirm ref={modalRef}
                             title="Confirm this"
                             onConfirm={onConfirm}
                             onCancel={onCancel}>
         Are you sure you want to do this?
      </BootstrapModalConfirm>
    </div>
  );
}

<BootstrapModalConfirmExample />
```
