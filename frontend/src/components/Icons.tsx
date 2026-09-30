import type { ReactNode, SVGProps } from "react";

type IconProps = SVGProps<SVGSVGElement>;

function Base(props: IconProps & { children: ReactNode }) {
  const { children, ...rest } = props;
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {children}
    </svg>
  );
}

/** Keystone arch mark: two voussoirs + the wedge keystone on top. */
export function KeystoneLogo({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 40 40" aria-hidden="true">
      <rect x="0.5" y="0.5" width="39" height="39" rx="9" fill="#F5A524" />
      <path d="M8 32V21a12 12 0 0 1 5.2-9.9L16.4 16A6.5 6.5 0 0 0 13.6 21v11Z" fill="#0F1C1D" />
      <path d="M32 32V21a12 12 0 0 0-5.2-9.9L23.6 16a6.5 6.5 0 0 1 2.8 5v11Z" fill="#0F1C1D" />
      <path d="M14.6 9.6 17.4 15h5.2l2.8-5.4A12 12 0 0 0 20 8.6a12 12 0 0 0-5.4 1Z" fill="#0F1C1D" />
    </svg>
  );
}

export const IconDashboard = (p: IconProps) => (
  <Base {...p}>
    <rect x="3" y="3" width="7" height="9" rx="1.5" />
    <rect x="14" y="3" width="7" height="5" rx="1.5" />
    <rect x="14" y="12" width="7" height="9" rx="1.5" />
    <rect x="3" y="16" width="7" height="5" rx="1.5" />
  </Base>
);

export const IconBuilding = (p: IconProps) => (
  <Base {...p}>
    <path d="M4 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16" />
    <path d="M16 9h2a2 2 0 0 1 2 2v10" />
    <path d="M8 7h4M8 11h4M8 15h4M3 21h18" />
  </Base>
);

export const IconPin = (p: IconProps) => (
  <Base {...p}>
    <path d="M12 21s-7-6.1-7-11.5A7 7 0 0 1 19 9.5C19 14.9 12 21 12 21Z" />
    <circle cx="12" cy="9.5" r="2.5" />
  </Base>
);

export const IconClipboard = (p: IconProps) => (
  <Base {...p}>
    <rect x="5" y="4" width="14" height="17" rx="2" />
    <path d="M9 4V3h6v1M9 11h6M9 15h4" />
  </Base>
);

export const IconUsers = (p: IconProps) => (
  <Base {...p}>
    <circle cx="9" cy="8" r="3.5" />
    <path d="M2.5 20a6.5 6.5 0 0 1 13 0" />
    <path d="M16 4.5a3.5 3.5 0 0 1 0 7M18 14a6 6 0 0 1 3.5 6" />
  </Base>
);

export const IconHeadset = (p: IconProps) => (
  <Base {...p}>
    <path d="M4 14v-2a8 8 0 0 1 16 0v2" />
    <rect x="3" y="14" width="4" height="6" rx="1.5" />
    <rect x="17" y="14" width="4" height="6" rx="1.5" />
    <path d="M19 20a3 3 0 0 1-3 2h-3" />
  </Base>
);

export const IconWrench = (p: IconProps) => (
  <Base {...p}>
    <path d="M14.7 6.3a4 4 0 0 0 5 5L21 13l-8 8-3-3 8-8-1.3-1.3a4 4 0 0 1-5-5L13 3l1.7 3.3Z" />
    <path d="m3 21 6-6" />
  </Base>
);

export const IconChart = (p: IconProps) => (
  <Base {...p}>
    <path d="M3 3v18h18" />
    <path d="m7 15 4-4 3 3 5-6" />
  </Base>
);

export const IconUser = (p: IconProps) => (
  <Base {...p}>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21a8 8 0 0 1 16 0" />
  </Base>
);

export const IconFan = (p: IconProps) => (
  <Base {...p}>
    <circle cx="12" cy="12" r="1.8" />
    <path d="M12 10.2C12 6 13 3 15.5 3s3 3.5-3.5 7.2ZM13.8 12c4.2 0 7.2 1 7.2 3.5s-3.5 3-7.2-3.5ZM12 13.8c0 4.2-1 7.2-3.5 7.2s-3-3.5 3.5-7.2ZM10.2 12C6 12 3 11 3 8.5s3.5-3 7.2 3.5Z" />
  </Base>
);

export const IconBolt = (p: IconProps) => (
  <Base {...p}>
    <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8Z" />
  </Base>
);

export const IconDrop = (p: IconProps) => (
  <Base {...p}>
    <path d="M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11Z" />
  </Base>
);

export const IconLock = (p: IconProps) => (
  <Base {...p}>
    <rect x="5" y="11" width="14" height="10" rx="2" />
    <path d="M8 11V8a4 4 0 0 1 8 0v3" />
  </Base>
);
