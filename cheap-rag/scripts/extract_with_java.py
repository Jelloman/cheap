#!/usr/bin/env python3
"""
Python wrapper script for invoking the Java metadata extractor.

This script allows the Python-based cheap-rag system to use the higher-quality
Java extractor for extracting metadata from Java source code.

Usage:
    from cheap_rag.scripts.extract_with_java import extract_with_java

    artifacts = extract_with_java(
        source_path="src/main/java",
        jar_path="cheap-rag.jar",
        public_only=True
    )
"""

import json
import subprocess
import sys
from pathlib import Path
from typing import List, Optional


def extract_with_java(
    source_path: str,
    jar_path: str,
    output_file: Optional[str] = None,
    public_only: bool = False,
    extract_classes: bool = True,
    extract_interfaces: bool = True,
    extract_enums: bool = True,
    extract_methods: bool = True,
    extract_fields: bool = True,
) -> List[dict]:
    """
    Extract metadata from Java source code using the Java extractor.

    Args:
        source_path: Path to Java source file or directory
        jar_path: Path to cheap-rag.jar
        output_file: Optional path to write JSON output (default: use stdout)
        public_only: Extract only public API (default: False)
        extract_classes: Extract classes (default: True)
        extract_interfaces: Extract interfaces (default: True)
        extract_enums: Extract enums (default: True)
        extract_methods: Extract methods (default: True)
        extract_fields: Extract fields (default: True)

    Returns:
        List of metadata artifact dictionaries

    Raises:
        FileNotFoundError: If jar_path or source_path doesn't exist
        subprocess.CalledProcessError: If Java extractor fails
        json.JSONDecodeError: If output JSON is invalid
    """
    # Validate paths
    jar_path_obj = Path(jar_path)
    source_path_obj = Path(source_path)

    if not jar_path_obj.exists():
        raise FileNotFoundError(f"JAR not found: {jar_path}")

    if not source_path_obj.exists():
        raise FileNotFoundError(f"Source path not found: {source_path}")

    # Build command
    cmd = ["java", "-jar", str(jar_path_obj), "extract", str(source_path_obj)]

    # Add options
    if public_only:
        cmd.append("--public-only")

    if not extract_classes:
        cmd.append("--no-classes")

    if not extract_interfaces:
        cmd.append("--no-interfaces")

    if not extract_enums:
        cmd.append("--no-enums")

    if not extract_methods:
        cmd.append("--no-methods")

    if not extract_fields:
        cmd.append("--no-fields")

    # Run extractor
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            check=True,
        )
    except subprocess.CalledProcessError as e:
        print(f"Error running Java extractor: {e.stderr}", file=sys.stderr)
        raise

    # Parse JSON output
    try:
        artifacts = json.loads(result.stdout)
    except json.JSONDecodeError as e:
        print(f"Error parsing JSON output: {e}", file=sys.stderr)
        print(f"Output was: {result.stdout[:500]}", file=sys.stderr)
        raise

    # Optionally write to file
    if output_file:
        output_path = Path(output_file)
        with output_path.open("w") as f:
            json.dump(artifacts, f, indent=2)

    return artifacts


def main():
    """Command-line interface for testing the wrapper."""
    import argparse

    parser = argparse.ArgumentParser(
        description="Extract Java metadata using Java extractor"
    )
    parser.add_argument("source_path", help="Path to Java source file or directory")
    parser.add_argument("--jar", default="cheap-rag.jar", help="Path to JAR file")
    parser.add_argument("-o", "--output", help="Output JSON file")
    parser.add_argument(
        "--public-only", action="store_true", help="Extract only public API"
    )
    parser.add_argument(
        "--no-methods", action="store_true", help="Skip method extraction"
    )
    parser.add_argument(
        "--no-fields", action="store_true", help="Skip field extraction"
    )

    args = parser.parse_args()

    try:
        artifacts = extract_with_java(
            source_path=args.source_path,
            jar_path=args.jar,
            output_file=args.output,
            public_only=args.public_only,
            extract_methods=not args.no_methods,
            extract_fields=not args.no_fields,
        )

        print(f"Extracted {len(artifacts)} artifacts", file=sys.stderr)

        if not args.output:
            print(json.dumps(artifacts, indent=2))

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
