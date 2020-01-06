### Default

```js
<Alert>
  <i className="fa fa-exclamation-triangle fa-fw fa-lg" />{' '}
  <strong>Default</strong> Lorem ipsum dolor sit amet consectetur adipisicing elit.
</Alert>
```

### Variants

```js
const styles = ['Danger', 'Info', 'Success', 'Warning'];

styles.map((style, i) => {
  return (
    <Alert bsStyle={style.toLowerCase()} key={`button-${style}-${i}`}>
      <i className="fa fa-exclamation-triangle fa-fw fa-lg" />{' '}
      <strong>{style}</strong> Lorem ipsum dolor sit amet consectetur adipisicing elit.
    </Alert>
  )
})
```
