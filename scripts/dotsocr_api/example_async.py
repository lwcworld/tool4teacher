"""
Asynchronous usage example for DotsOCR API
"""

import os
import asyncio
from dotenv import load_dotenv
from dotsocr_client import AsyncDotsOCRClient, PromptMode, DotsOCRError

# Load environment variables
load_dotenv()


async def example_single_file():
    """Example: Process a single file"""
    print("\n=== Example 1: Process a single file ===")

    api_key = os.getenv("DOTS_OCR_API_KEY")
    if not api_key:
        raise ValueError("DOTS_OCR_API_KEY not found in environment variables")

    async with AsyncDotsOCRClient(api_key=api_key) as client:
        try:
            file_url = "https://file.302ai.cn/gpt/imgs/20250822/72b0a6687ee3a59071038982f8931ecc.pdf"

            # Process file (creates task and waits for completion)
            result = await client.process_file(
                file_url=file_url,
                prompt_mode=PromptMode.PROMPT_GROUNDING_OCR,
                temperature=0.1,
                top_p=1.0
            )

            print(f"OCR Result: {result}")

            if "text" in result:
                print(f"\nExtracted text: {result['text']}")

        except DotsOCRError as e:
            print(f"Error: {e}")
        except TimeoutError as e:
            print(f"Timeout: {e}")


async def example_multiple_files():
    """Example: Process multiple files concurrently"""
    print("\n=== Example 2: Process multiple files concurrently ===")

    api_key = os.getenv("DOTS_OCR_API_KEY")
    if not api_key:
        raise ValueError("DOTS_OCR_API_KEY not found in environment variables")

    file_urls = [
        "https://file.302ai.cn/gpt/imgs/20250822/72b0a6687ee3a59071038982f8931ecc.pdf",
        # Add more URLs here
    ]

    async with AsyncDotsOCRClient(api_key=api_key) as client:
        # Create all tasks concurrently
        tasks = []
        for url in file_urls:
            task = client.process_file(
                file_url=url,
                prompt_mode=PromptMode.PROMPT_GROUNDING_OCR
            )
            tasks.append(task)

        # Wait for all tasks to complete
        try:
            results = await asyncio.gather(*tasks, return_exceptions=True)

            for i, result in enumerate(results):
                print(f"\n--- File {i+1} ---")
                if isinstance(result, Exception):
                    print(f"Error: {result}")
                else:
                    print(f"Result: {result}")

        except Exception as e:
            print(f"Error processing files: {e}")


async def example_manual_polling():
    """Example: Manual task creation and polling"""
    print("\n=== Example 3: Manual task creation and polling ===")

    api_key = os.getenv("DOTS_OCR_API_KEY")
    if not api_key:
        raise ValueError("DOTS_OCR_API_KEY not found in environment variables")

    async with AsyncDotsOCRClient(api_key=api_key) as client:
        try:
            file_url = "https://file.302ai.cn/gpt/imgs/20250822/72b0a6687ee3a59071038982f8931ecc.pdf"

            # Create task
            task = await client.create_task(
                file_url=file_url,
                prompt_mode="prompt_grounding_ocr"
            )

            print(f"Task created: {task}")

            task_id = task.get("task_id")
            if task_id:
                print(f"Task ID: {task_id}")

                # Wait for completion
                result = await client.wait_for_completion(task_id)
                print(f"Task completed: {result}")

        except DotsOCRError as e:
            print(f"Error: {e}")


async def main():
    """Run all examples"""
    await example_single_file()
    await example_multiple_files()
    await example_manual_polling()


if __name__ == "__main__":
    asyncio.run(main())
