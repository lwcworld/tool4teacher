"""
Core logic for splitting OCR output by problem numbers.
"""

import json
import re
import sys
from typing import List, Dict, Tuple


def extract_problem_number(text: str) -> int:
    """
    Extract problem number from text.
    Matches patterns like "1.", "5. ", "10. ", etc.

    Args:
        text: Text to extract problem number from

    Returns:
        Problem number or -1 if not found
    """
    # Match problem number at the start of text (e.g., "1.", "5. ", "10. ")
    # Space after dot is optional to handle cases like "1." and "5. 다항함수"
    match = re.match(r'^(\d+)\.\s*', text)
    if match:
        return int(match.group(1))
    return -1


def find_problem_markers(elements: List[Dict]) -> List[Tuple[int, int, int, int]]:
    """
    Find problem number markers in the JSON elements.

    Args:
        elements: List of JSON elements from dotsocr

    Returns:
        List of tuples (index, problem_number, x_position, y_position)
    """
    markers = []

    for idx, elem in enumerate(elements):
        # Skip page headers/footers (but NOT section headers - they may contain problem numbers)
        if elem.get('category') in ['Page-header', 'Page-footer']:
            continue

        text = elem.get('text', '')
        problem_num = extract_problem_number(text)

        if problem_num > 0:
            # Get x and y position from bbox
            if 'bbox' in elem:
                x_pos = elem['bbox'][0]  # left coordinate
                y_pos = elem['bbox'][1]  # top coordinate
                markers.append((idx, problem_num, x_pos, y_pos))

    return markers


def split_by_problems(elements: List[Dict]) -> Dict[int, List[Dict]]:
    """
    Split elements by problem numbers.

    Uses spatial proximity to assign elements to problems. Each element is assigned
    to the nearest problem marker based on spatial distance (considering both x and y).

    Args:
        elements: List of JSON elements from dotsocr

    Returns:
        Dictionary mapping problem numbers to their elements
    """
    markers = find_problem_markers(elements)

    if not markers:
        print("  Warning: No problem markers found in this page", file=sys.stderr)
        return {}

    # Sort markers by problem number
    markers.sort(key=lambda x: x[1])

    # Initialize problems dict
    problems = {marker[1]: [] for marker in markers}

    # Assign each element to the nearest problem
    for idx, elem in enumerate(elements):
        category = elem.get('category', '')

        # Skip page headers and footers
        if category in ['Page-header', 'Page-footer']:
            continue

        if 'bbox' not in elem:
            continue

        elem_x = elem['bbox'][0]  # left coordinate
        elem_y = elem['bbox'][1]  # top coordinate

        # Find the nearest problem marker
        min_distance = float('inf')
        nearest_problem = None

        for marker_idx, problem_num, marker_x, marker_y in markers:
            # Calculate distance with more weight on y-axis for better column separation
            # If elements are in different columns (large x difference), they shouldn't be grouped
            x_diff = abs(elem_x - marker_x)
            y_diff = abs(elem_y - marker_y)

            # If x difference is too large (> 300 pixels), heavily penalize it
            # This prevents elements from different columns being grouped together
            if x_diff > 300:
                distance = float('inf')
            else:
                # Normal distance calculation with slight y-axis preference
                distance = (x_diff * 0.5) + y_diff

            # Only consider elements that are at or below the marker (not above)
            # Allow small tolerance for alignment issues
            if elem_y >= marker_y - 50:
                if distance < min_distance:
                    min_distance = distance
                    nearest_problem = problem_num

        # Assign to nearest problem
        if nearest_problem is not None:
            problems[nearest_problem].append(elem)

    return problems
