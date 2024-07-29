Simple usage:
```tsx
import styled from 'styled-components';
import { Icon } from 'components/common';

const DropzoneInner = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
`;


<Dropzone onDrop={() => alert('File dropped!')}
          onReject={() => alert('File rejected!')}
          accept={['image/png', 'image/jpeg']}
          maxSize={1024 * 1024}
          loading={false}>
  <DropzoneInner>
    <Dropzone.Accept>
      <Icon name="image" type="regular" size="2x" />
    </Dropzone.Accept>
    <Dropzone.Reject>
      <Icon name="warning" size="2x" />
    </Dropzone.Reject>
    <Dropzone.Idle>
      <Icon name="image" type="regular" size="2x" />
    </Dropzone.Idle>
    <div>Drag an image here or click to select file</div>
  </DropzoneInner>
</Dropzone>
```
