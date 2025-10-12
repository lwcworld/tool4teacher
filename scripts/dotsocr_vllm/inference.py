"""
vLLM inference script for dots.ocr model.
This script provides inference functionality using the dots.ocr model deployed with vLLM.
"""

import argparse
import sys
from pathlib import Path
from typing import Optional

# Add third_party/dots.ocr to Python path
repo_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(repo_root / "third_party" / "dots.ocr"))

from openai import OpenAI
from PIL import Image
from dots_ocr.utils import dict_promptmode_to_prompt
from dots_ocr.model.inference import inference_with_vllm


def run_inference(
    image_path: str,
    prompt_mode: str = "prompt_layout_all_en",
    ip: str = "localhost",
    port: int = 8000,
    model_name: str = "model",
    temperature: float = 0.1,
    top_p: float = 0.9,
    max_tokens: int = 32768,
) -> Optional[str]:
    """
    Run inference on an image using vLLM-deployed dots.ocr model.

    Args:
        image_path: Path to the image file
        prompt_mode: One of the available prompt modes
        ip: Server IP address
        port: Server port
        model_name: Model name registered with vLLM
        temperature: Sampling temperature
        top_p: Top-p sampling parameter
        max_tokens: Maximum completion tokens

    Returns:
        Model response string or None if error occurs
    """
    # Validate image path
    if not Path(image_path).exists():
        print(f"Error: Image file not found: {image_path}")
        return None

    # Get prompt from mode
    if prompt_mode not in dict_promptmode_to_prompt:
        print(f"Error: Invalid prompt mode '{prompt_mode}'")
        print(f"Available modes: {list(dict_promptmode_to_prompt.keys())}")
        return None

    prompt = dict_promptmode_to_prompt[prompt_mode]

    # Load image
    try:
        image = Image.open(image_path)
    except Exception as e:
        print(f"Error loading image: {e}")
        return None

    # Run inference
    print(f"Running inference on: {image_path}")
    print(f"Using prompt mode: {prompt_mode}")
    print(f"Server: {ip}:{port}")

    response = inference_with_vllm(
        image,
        prompt,
        ip=ip,
        port=port,
        temperature=temperature,
        top_p=top_p,
        max_completion_tokens=max_tokens,
        model_name=model_name,
    )

    return response


def main():
    parser = argparse.ArgumentParser(
        description="Run vLLM inference with dots.ocr model",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Available prompt modes:
  prompt_layout_all_en    - Parse all layout info (bbox, category, text) in JSON format
  prompt_layout_only_en   - Layout detection only (no text extraction)
  prompt_ocr              - Extract all text content
  prompt_grounding_ocr    - Extract text from given bounding box

Examples:
  # Basic usage with default settings
  python inference.py --image path/to/image.jpg

  # Layout detection only
  python inference.py --image path/to/image.jpg --prompt_mode prompt_layout_only_en

  # Connect to remote server
  python inference.py --image path/to/image.jpg --ip 192.168.1.100 --port 8000
        """
    )

    parser.add_argument(
        "--image",
        type=str,
        required=True,
        help="Path to input image file"
    )
    parser.add_argument(
        "--prompt_mode",
        type=str,
        default="prompt_layout_all_en",
        choices=list(dict_promptmode_to_prompt.keys()),
        help="Prompt mode to use (default: prompt_layout_all_en)"
    )
    parser.add_argument(
        "--ip",
        type=str,
        default="localhost",
        help="vLLM server IP address (default: localhost)"
    )
    parser.add_argument(
        "--port",
        type=int,
        default=8000,
        help="vLLM server port (default: 8000)"
    )
    parser.add_argument(
        "--model_name",
        type=str,
        default="model",
        help="Model name registered with vLLM (default: model)"
    )
    parser.add_argument(
        "--temperature",
        type=float,
        default=0.1,
        help="Sampling temperature (default: 0.1)"
    )
    parser.add_argument(
        "--top_p",
        type=float,
        default=0.9,
        help="Top-p sampling parameter (default: 0.9)"
    )
    parser.add_argument(
        "--max_tokens",
        type=int,
        default=32768,
        help="Maximum completion tokens (default: 32768)"
    )
    parser.add_argument(
        "--output",
        type=str,
        help="Output file path (optional, prints to stdout if not specified)"
    )

    args = parser.parse_args()

    # Run inference
    response = run_inference(
        image_path=args.image,
        prompt_mode=args.prompt_mode,
        ip=args.ip,
        port=args.port,
        model_name=args.model_name,
        temperature=args.temperature,
        top_p=args.top_p,
        max_tokens=args.max_tokens,
    )

    if response is None:
        sys.exit(1)

    # Output results
    print("\n" + "="*80)
    print("RESPONSE:")
    print("="*80)
    print(response)

    if args.output:
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(response)
        print(f"\nResponse saved to: {args.output}")


if __name__ == "__main__":
    main()
