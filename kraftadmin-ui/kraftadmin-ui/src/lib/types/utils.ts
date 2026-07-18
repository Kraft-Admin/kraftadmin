/**
 * Represents an object that can be displayed in the UI.
 */

export interface LabelledObject {
    id: string;
    label: string;
}

export const FILE_INPUT_TYPES = new Set(['IMAGE', 'VIDEO', 'AUDIO', 'FILE', 'DOCUMENT']);

export function isFileInputType(inputType?: string | null): boolean {
  return !!inputType && FILE_INPUT_TYPES.has(inputType);
}