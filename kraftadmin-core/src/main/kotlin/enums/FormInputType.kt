package com.kraftadmin.enums

enum class FormInputType {
    // Basic Inputs
    TEXT, TEXTAREA, NUMBER, EMAIL, PASSWORD, TEL, URL, SEARCH, HIDDEN,

    // Date/Time
    DATE, DATETIME, TIME,

    // Selection/Boolean
    SELECT, MULTI_SELECT, RADIO, CHECKBOX,

    // Media & Files
    IMAGE, VIDEO, AUDIO, FILE, DOCUMENT,

    // Complex Data
    ARRAY, OBJECT, RELATION, MULTI_RELATION, JSON, WYSIWYG,

    // Styling/Special
    COLOR, RANGE,

    UNSET
}
