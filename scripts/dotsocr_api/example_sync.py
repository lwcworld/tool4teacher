"""
Synchronous usage example for DotsOCR API
"""

import os
from dotenv import load_dotenv
from dotsocr_client import DotsOCRClient, DotsOCRError

# Load environment variables
load_dotenv()


def main():
    # Initialize client
    api_key = os.getenv("DOTS_OCR_API_KEY")
    if not api_key:
        raise ValueError("DOTS_OCR_API_KEY not found in environment variables")

    client = DotsOCRClient(api_key=api_key)

    # Example 2: Create task and wait for completion
    print("\n=== Example 2: Create task and wait for completion ===")
    try:
        file_url = "https://file.302ai.cn/gpt/imgs/20250822/72b0a6687ee3a59071038982f8931ecc.pdf"

        # Create task
        task = client.create_task(
            file_url=file_url,
            prompt_mode="prompt_grounding_ocr"
        )

        task_id = task.get("task_id")
        if task_id:
            print(f"Task ID: {task_id}")
            print("Waiting for task to complete...")

            # Wait for completion (polls every 2 seconds, max 300 seconds)
            result = client.wait_for_completion(
                task_id=task_id,
                max_wait_time=300,
                poll_interval=10
            )

            print(f"Task completed!")
            print(f"Result: {result}")

    except DotsOCRError as e:
        print(f"Error: {e}")
    except TimeoutError as e:
        print(f"Timeout: {e}")


if __name__ == "__main__":
    main()
