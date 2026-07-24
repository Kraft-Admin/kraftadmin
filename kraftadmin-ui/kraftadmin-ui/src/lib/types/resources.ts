// Enums
export enum ValueType {
  STRING = "STRING",
  INT = "INT",
  LONG = "LONG",
  DOUBLE = "DOUBLE",
  BOOLEAN = "BOOLEAN",
  UUID = "UUID",
  ENUM = "ENUM",
  EMBEDDABLE = "EMBEDDABLE",
  DATE = "DATE",
  DATETIME = "DATETIME",
  COLLECTION = "COLLECTION",
}

export enum ElementCollectionShape {
  LIST = "LIST",
  SET = "SET",
  MAP = "MAP",
}

export enum ActionVariant {
  DEFAULT = "DEFAULT",
  PRIMARY = "PRIMARY",
  SUCCESS = "SUCCESS",
  WARNING = "WARNING",
  DANGER = "DANGER",
  ERROR = "ERROR",
}

export enum ActionTarget {
  ROW = "ROW",
  BULK = "BULK",
  GLOBAL = "GLOBAL",
}

export enum WysiwygToolbar {
  STANDARD = "STANDARD",
  MINIMAL = "MINIMAL",
  FULL = "FULL",
}

// Column & Field Primitive Types

export type ColumnType =
  | "TEXT" | "TEXTAREA" | "NUMBER" | "EMAIL" | "PASSWORD" | "TEL" | "URL"
  | "SEARCH" | "HIDDEN" | "DATE" | "DATETIME" | "TIME" | "SELECT" | "MULTI_SELECT"
  | "RADIO" | "CHECKBOX" | "IMAGE" | "VIDEO" | "AUDIO" | "FILE" | "DOCUMENT"
  | "ARRAY" | "OBJECT" | "RELATION" | "MULTI_RELATION" | "JSON" | "WYSIWYG"
  | "COLLECTION" | "COLOR" | "RANGE" | "UNSET";

// Shared Leaf Interfaces

export interface SelectOption {
  label: string;
  value: string;
}

export interface FileOptionsDescriptor {
  allowedExtensions: string[];
  allowedMimeTypes: string[];
  maxFiles: number;
  maxSizeBytes: number;
  minSizeBytes: number;
  multiple: boolean;
}

export interface LookupDescriptor {
  displayField: string;
  lookupKey: string;
  searchableFields: string[];
  targetEntity: string;
}

// Recursive Descriptors

export interface ElementCollectionDescriptor {
  shape: ElementCollectionShape;
  key?: ValueDescriptor | null;
  value: ValueDescriptor;
  minItems?: number | null;
  maxItems?: number | null;
}

export interface EmbeddableFieldDescriptor {
  name: string;
  label: string;
  required: boolean;
  placeholder?: string | null;
  value: ValueDescriptor;
  inputType: ColumnType | string;
}

export interface ValueDescriptor {
  type: ValueType;
  className?: string | null;
  nullable: boolean;
  defaultValue?: unknown;
  enumValues: SelectOption[];
  fields: EmbeddableFieldDescriptor[];
  collection?: ElementCollectionDescriptor | null;
  inputType: ColumnType | string;
  fileOptions: FileOptionsDescriptor | null;
}

// WYSIWYG

export type WysiwygToolbarButton =
  | string
  | { header: 1 | 2 | 3 | 4 | 5 | 6 }
  | { list: "ordered" | "bullet" };

export interface WysiwygConfig {
  options: WysiwygToolbarButton[][];
  placeholder: string;
  toolbar: WysiwygToolbar;
}

// Column Metadata

export interface KraftAdminColumn {
  defaultValue: unknown;
  elementCollection: ElementCollectionDescriptor | null;
  error: string | null;
  fileOptions: FileOptionsDescriptor | null;
  label: string;
  lookup: LookupDescriptor | null;
  name: string;
  placeholder: string | undefined;
  required: boolean;
  searchable: boolean;
  selectOptions: SelectOption[] | null;
  showInTable: boolean;
  sortable: boolean;
  subColumns: KraftAdminColumn[] | null;
  type: ColumnType;
  validationMessages: Record<string, string> | null;
  validationRules: string | null;
  visible: boolean;
  wysiwygConfig: WysiwygConfig | null;
}

// Custom Actions

export interface CustomActionInputField {
  defaultValue: unknown;
  file: FileOptionsDescriptor | null;
  helperText: string | null;
  hidden: boolean;
  label: string;
  max: number | null;
  maxLength: number | null;
  min: number | null;
  minLength: number | null;
  name: string;
  options: SelectOption[];
  order: number;
  pattern: string | null;
  placeholder: string | null;
  readOnly: boolean;
  required: boolean;
  type: ColumnType | "WYSIWYG";
}

export interface CustomActionInput {
  cancelLabel: string;
  className: string;
  description: string;
  fields: CustomActionInputField[];
  submitLabel: string;
  title: string;
}

export interface CustomAction {
  bulk: boolean;
  confirmMessage: string | null;
  entityClass: string;
  group: string | null;
  hideAfterExecution: boolean;
  icon: string;
  input: CustomActionInput | null;
  label: string;
  name: string;
  order: number;
  permission: string | null;
  refresh: boolean;
  requiresSelection: boolean;
  target: ActionTarget;
  variant: ActionVariant;
}

// Row Data & Related Resources

export interface ResourceRowMetadata {
  canEdit: boolean;
  canDelete: boolean;
  cssClass: string | null;
}

export interface ObjectResponse {
  id: string;
  label: string;
}

export interface RelatedItem {
  id: string;
  entityType: string;
  displayLabel: string;
  values: Record<string, any>;
}

export interface RelatedCollection {
  entityType: string;
  fieldName: string;
  items: RelatedItem[];
  limited: boolean;
  lookupDescriptor: LookupDescriptor;
  totalInMemory: number;
}

export type RelatedResources = Record<string, RelatedCollection>;

export interface ResourceRow {
  id: string;
  values: Record<string, unknown>;
  metadata: ResourceRowMetadata;
  relatedResources?: RelatedResources | null;
  customActions: CustomAction[];
}

// API Responses

export interface PagedResourceData {
  items: ResourceRow[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
}

export interface KraftAdminResource {
  name: string;
  label: string;
  group: string;
  icon: string;
  hidden: boolean;
  searchable: boolean;
  defaultSort: string;
  readOnly: boolean;
  pageSize: number;
  permissionScope: string;
  exportable: boolean;
  totalCount: number;
  isSystem?: boolean;
  columns: KraftAdminColumn[];
  customActions: CustomAction[];
  data: PagedResourceData;
  searchableFields: string[];
  sortableFields: string[];
  provider: string;
}

export interface KraftOperationResponse<T> {
  success: boolean;
  message: string | null;
  data: T | null;
  errors: Record<string, string[]>;
}

// Bootstrap/Descriptors

export interface CurrentUser {
  avatar: string | null;
  bridgeMode: boolean;
  initials: string;
  name: string;
  roles: string[];
  username: string;
}

export interface EnvironmentInfo {
  authMode: "bridge" | "standalone" | "unknown";
  features: { allowDelete: boolean; readOnly: boolean; showTimestamps: boolean };
  locale: { defaultLanguage: string; timezone: string };
  name: string;
  pagination: { defaultPageSize: number; maxPageSize: number };
  showLogout: boolean;
  theme: { darkMode: boolean; logoUrl: string | null; primaryColor: string };
  version: string;
}

export interface DescriptorsResponse {
  basePath: string;
  currentUser: CurrentUser;
  environment: EnvironmentInfo;
  resources: KraftAdminResource[];
  title: string;
  version: string;
}