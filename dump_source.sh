#!/bin/sh

# Target output file on the SD card
OUTPUT_FILE="/sdcard/source.txt"

# Clear the file if it already exists
> "$OUTPUT_FILE"

# Find all files, excluding build directories, hidden folders, and common binaries
find . -type f ! -path "*/build/*" ! -path "*/.*/*" | while read -r file; do
    
    # Skip common binary file extensions
    case "$file" in
        *.png|*.jpg|*.jpeg|*.gif|*.jar|*.zip|*.apk|*.aar|*.so|*.class)
            continue
            ;;
    esac

    # Ensure the file is readable
    if [ -r "$file" ]; then
        # Extract path and name
        FULL_PATH="$PWD/${file#./}"
        FILE_NAME=$(basename "$file")

        # Append to the single source text file
        printf "File path: %s\n" "$FULL_PATH" >> "$OUTPUT_FILE"
        printf "File name: %s\n" "$FILE_NAME" >> "$OUTPUT_FILE"
        printf "Content:\n" >> "$OUTPUT_FILE"
        cat "$file" >> "$OUTPUT_FILE"
        
        # Spacing separator between files
        printf "\n\n" >> "$OUTPUT_FILE"
    fi
done

echo "Successfully compiled all source files into: $OUTPUT_FILE"
