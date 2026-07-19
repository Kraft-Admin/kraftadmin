import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// 1. Locate the package directory and icons
const lucidePath = path.resolve(__dirname, '../node_modules/lucide-svelte');
const iconsDir = path.join(lucidePath, 'dist/icons');

if (!fs.existsSync(iconsDir)) {
    console.error(`Error: Icons directory not found at ${iconsDir}`);
    process.exit(1);
}

const iconFiles = fs.readdirSync(iconsDir);

const iconNames = iconFiles
    .filter(file => file.endsWith('.js'))
    .map(file => path.basename(file, '.js'));

// Generate Constants for Annotations
// Change the generation logic to output simple String constants
const constants = iconNames.map(name => {
    const constantName = name.replace(/-/g, '_').toUpperCase();
    // These are now 'const val' Strings, not part of the Enum instance
    return `        const val ICON_${constantName} = "${name}"`;
}).join('\n');

// Generate Enum Entries
const enumEntries = iconNames
    .filter(name => name && name.length > 0)
    .map(name => {
        let constantName = name.replace(/-/g, '_').toUpperCase();
        if (/^\d/.test(constantName)) constantName = `_${constantName}`;
        return `    ${constantName}("${name}")`;
    })
    .join(',\n');

const kotlinCode = `package com.kraftadmin.enums

enum class KraftIcon(val iconName: String) {
${enumEntries};

    companion object {
        // These constants are now accessible to @KraftAdminResource
${constants}

        const val DEFAULT_ICON = "Folder"

        fun fromString(name: String?): String {
            return try {
                entries.find { it.iconName.equals(name, true) }?.iconName ?: DEFAULT_ICON
            } catch (e: Exception) {
                DEFAULT_ICON
            }
        }
    }
}
`;

// 4. Write the file with directory creation
const targetDir = path.resolve(__dirname, '../../../kraftadmin-core/src/main/kotlin/enums');
const targetPath = path.join(targetDir, 'KraftIcon.kt');

if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
}

fs.writeFileSync(targetPath, kotlinCode);
console.log(`Successfully generated KraftIcon.kt at ${targetPath} with ${iconNames.length} icons.`);