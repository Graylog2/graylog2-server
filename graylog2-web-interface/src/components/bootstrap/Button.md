#### Variants
```tsx
const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Link', 'Default', 'Transparent'];

styles.map((style, i) => {
  return (
    <p key={`button-example-${style}-${i}`}>
      <Button bsStyle={style.toLowerCase()}>{style}</Button>{' '}
      <Button active bsStyle={style.toLowerCase()}>{style} Active</Button>{' '}
      <Button disabled bsStyle={style.toLowerCase()}>{style} Disabled</Button>{' '}
    </p>
  )
})
```

#### Sizes
```tsx
const sizes = ['xs' , 'sm' , 'md' , 'lg'];

sizes.map((size, i) => {
  return (
    <p key={`button-example-${size}-${i}`}>
      <Button bsStyle="info" bsSize={size}>{size}</Button>{' '}
    </p>
  )
})
```

#### Button with a link
```tsx
<Button href="https://www.graylog.org"
        target="_blank"
        rel="noopener noreferrer">
  graylog.org
</Button>
```
