import {
    ValueType,
    type ValueDescriptor
} from "../../lib/types/descriptors";

export function createDefaultValue(descriptor: ValueDescriptor): any {

    if (descriptor.collection) {
        return [];
    }

    switch (descriptor.type) {

        case ValueType.EMBEDDABLE: {

            const obj: Record<string, any> = {};

            for (const field of descriptor.fields) {
                obj[field.name] = createDefaultValue(field.value);
            }

            return obj;
        }

        case ValueType.BOOLEAN:
            return false;

        default:
            return null;
    }
}