export interface KraftResource {
  name: string;
  label: string;
  group: string;
  icon: string;
  isHidden: boolean;
  isSearchable: boolean;
  defaultSort: string;
  isReadOnly: boolean;
  pageSize: number;
  permissionScope: string;
  isExportable: boolean;
  totalCount: number; // Added since you use this in your template
  isSystem?: boolean; // Optional flag for system resources
}