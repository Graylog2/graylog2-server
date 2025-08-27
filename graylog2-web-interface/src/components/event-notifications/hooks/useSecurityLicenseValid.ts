import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

export default function useSecurityLicenseValid(): boolean {
  const { data } = usePluggableLicenseCheck('/license/security');

  return Boolean(data?.valid);
}
