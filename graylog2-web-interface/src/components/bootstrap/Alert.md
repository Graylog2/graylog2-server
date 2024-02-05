#### Default

```tsx
<Alert>
  Default - Lorem ipsum dolor sit amet consectetur adipisicing elit.
</Alert>
```

#### Variants

```tsx
const styles = ['Danger', 'Info', 'Success', 'Warning'];

styles.map((style, i) => {
  return (
    <Alert bsStyle={style.toLowerCase()} key={`button-${style}-${i}`}>
      {style} - Lorem ipsum dolor sit amet consectetur adipisicing elit.
    </Alert>
  )
})
```


#### With Title

```tsx
<Alert title="The Title">
  Lorem ipsum dolor sit amet consectetur adipisicing elit.
</Alert>
```

#### With Close Button

```tsx
import Icon from 'components/common/Icon';

<Alert onDismiss={() => window.alert('You clicked on the alert close icon.')}>
  Lorem ipsum dolor sit amet consectetur adipisicing elit.
</Alert>
```
