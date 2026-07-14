package com.kraftadmin.enums

/**
 * Defines the supported input types for dynamic form generation in the KraftAdmin system.
 * This enum maps to specific frontend rendering components.
 */
enum class FormInputType {
    // Basic Inputs

    /** Single-line text input (standard input type="text"). */
    TEXT,
    /** Multi-line text area for long-form content. */
    TEXTAREA,
    /** Numeric input, typically used for integers or decimals. */
    NUMBER,
    /** Email address input with validation support. */
    EMAIL,
    /** Masked text input for sensitive data. */
    PASSWORD,
    /** Telephone number input. */
    TEL,
    /** URL format input. */
    URL,
    /** Search input field. */
    SEARCH,
    /** Input field hidden from the user interface. */
    HIDDEN,

    // Date/Time

    /** Calendar date selector (YYYY-MM-DD). */
    DATE,
    /** Date and time picker combination (ISO format). */
    DATETIME,
    /** Time-only selector (HH:mm). */
    TIME,

    //  Selection/Boolean

    /** Dropdown selection for a single value. */
    SELECT,
    /** Dropdown selection allowing multiple choices. */
    MULTI_SELECT,
    /** Set of radio buttons for choosing one of several options. */
    RADIO,
    /** Boolean toggle or checkbox input. */
    CHECKBOX,

    //  Media & Files

    /** Image file uploader with preview capabilities. */
    IMAGE,
    /** Video file uploader. */
    VIDEO,
    /** Audio file uploader. */
    AUDIO,
    /** Generic file uploader. */
    FILE,
    /** Specific document (PDF/Doc/Etc) uploader. */
    DOCUMENT,

    // Complex Data

    /** Input for managing lists of simple primitive values. */
    ARRAY,
    /** Complex nested structure input for JSON objects. */
    OBJECT,
    /** Link to a single related entity record. */
    RELATION,
    /** Link to multiple related entity records. */
    MULTI_RELATION,
    /** Raw JSON data input with formatting validation. */
    JSON,
    /** Rich text editor (WYSIWYG) for formatted HTML content. */
    WYSIWYG,
    COLLECTION,

    //  Styling/Special

    /** Color picker selector. */
    COLOR,
    /** Slider input for selecting a value within a specific range. */
    RANGE,

    /** Default state indicating no input type has been specified. */
    UNSET
}