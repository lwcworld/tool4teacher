#!/usr/bin/env python3
"""
Split dotsocr JSON output by problem numbers.
Each problem is saved as page_XXXX_YYYY.json where XXXX is page number and YYYY is problem number.
"""

import json
import re
import sys
from pathlib import Path

# Add parent directory to path to import utility modules
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from utility.problem_splitter import split_by_problems


def process_file(input_path: str, output_dir: str = None) -> None:
    """
    Process a single JSON file and split it by problems.

    Args:
        input_path: Path to input JSON file
        output_dir: Directory to save output files (default: same as input)
    """
    input_path = Path(input_path)

    if not input_path.exists():
        print(f"Error: File not found: {input_path}", file=sys.stderr)
        return

    # Determine output directory
    if output_dir is None:
        output_dir = input_path.parent
    else:
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)

    # Extract page number from filename (e.g., page_0001.json -> 0001)
    page_match = re.search(r'page_(\d+)\.json$', input_path.name)
    if not page_match:
        print(f"Error: Cannot extract page number from filename: {input_path.name}", file=sys.stderr)
        return

    page_num = page_match.group(1)

    # Load JSON
    print(f"Processing: {input_path.name}")
    with open(input_path, 'r', encoding='utf-8') as f:
        elements = json.load(f)

    # Split by problems
    problems = split_by_problems(elements)

    if not problems:
        print(f"  No problems found, skipping", file=sys.stderr)
        return

    # Sort problems by number
    sorted_problems = sorted(problems.items())

    # Save each problem to a separate file
    for problem_num, problem_elements in sorted_problems:
        output_filename = f"page_{page_num}_{problem_num:04d}.json"
        output_path = output_dir / output_filename

        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(problem_elements, f, ensure_ascii=False, indent=2)

        print(f"  Saved: {output_filename} ({len(problem_elements)} elements)")


def process_directory(input_dir: str, output_dir: str = None, pattern: str = "page_*.json") -> None:
    """
    Process all JSON files in a directory.

    Args:
        input_dir: Directory containing JSON files
        output_dir: Directory to save output files (default: same as input)
        pattern: Glob pattern for files to process
    """
    input_dir = Path(input_dir)

    if not input_dir.exists():
        print(f"Error: Directory not found: {input_dir}", file=sys.stderr)
        return

    # Find all matching files
    json_files = sorted(input_dir.glob(pattern))

    if not json_files:
        print(f"No files matching pattern '{pattern}' found in {input_dir}", file=sys.stderr)
        return

    print(f"Found {len(json_files)} files to process\n")

    for json_file in json_files:
        process_file(json_file, output_dir)
        print()


def main():
    """Main entry point."""
    if len(sys.argv) < 2:
        print("Usage:")
        print("  Split single file:     python split_problems.py <input_file.json> [output_dir]")
        print("  Split directory:       python split_problems.py <input_dir> [output_dir]")
        print()
        print("Examples:")
        print("  python split_problems.py dataset/downloads/suneung/page_0001.json")
        print("  python split_problems.py dataset/downloads/suneung/수학영역_문제지/")
        print("  python split_problems.py dataset/downloads/suneung/수학영역_문제지/ output/")
        sys.exit(1)

    input_path = Path(sys.argv[1])
    output_dir = sys.argv[2] if len(sys.argv) > 2 else None

    if input_path.is_file():
        process_file(str(input_path), output_dir)
    elif input_path.is_dir():
        process_directory(str(input_path), output_dir)
    else:
        print(f"Error: Not a file or directory: {input_path}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
