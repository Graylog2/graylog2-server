#### Default

```tsx
<SegmentedControl data={[
  {
    value: 'aloho',
    label: 'Aloho',
  },
  {
    value: 'lumos',
    label: 'Lumos',
  }
]}/>
```

#### Disabled

```tsx
<SegmentedControl data={[
  {
    value: 'aloho',
    label: 'Aloho',
  },
  {
    value: 'lumos',
    label: 'Lumos',
  }
]} disabled />
```

#### Single option disabled

```tsx
<SegmentedControl data={[
  {
    value: 'aloho',
    label: 'Aloho',
  },
  {
    value: 'mora',
    label: 'Mora',
    disabled: true
  },
  {
    value: 'lumos',
    label: 'Lumos',
  }
]} />
```

