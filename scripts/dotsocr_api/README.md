# DotsOCR API Client

Python wrapper for the 302.ai DotsOCR API. Supports both synchronous and asynchronous usage.

## Features

- **Synchronous and Asynchronous clients** - Choose the right one for your use case
- **Easy-to-use API** - Simple methods for common tasks
- **Automatic polling** - Wait for task completion automatically
- **Error handling** - Comprehensive error handling with custom exceptions
- **Type hints** - Full type annotations for better IDE support
- **Logging** - Built-in logging for debugging

## Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Set up your API key:
```bash
cp .env.example .env
# Edit .env and add your API key
```

## Quick Start

### Synchronous Usage

```python
from dotsocr_client import DotsOCRClient

# Initialize client
client = DotsOCRClient(api_key="your-api-key")

# Process a file (creates task and waits for completion)
result = client.process_file(
    file_url="https://example.com/document.pdf",
    prompt_mode="prompt_grounding_ocr"
)

print(result)
```

### Asynchronous Usage

```python
import asyncio
from dotsocr_client import AsyncDotsOCRClient

async def main():
    async with AsyncDotsOCRClient(api_key="your-api-key") as client:
        result = await client.process_file(
            file_url="https://example.com/document.pdf"
        )
        print(result)

asyncio.run(main())
```

## API Reference

### DotsOCRClient (Synchronous)

#### `__init__(api_key, timeout=30, use_https=True)`

Initialize the synchronous client.

**Parameters:**
- `api_key` (str): Your 302.ai API key
- `timeout` (int): Request timeout in seconds (default: 30)
- `use_https` (bool): Use HTTPS connection (default: True)

#### `create_task(file_url, prompt_mode="prompt_grounding_ocr", temperature=0.1, top_p=1.0, **kwargs)`

Create a new OCR task.

**Parameters:**
- `file_url` (str): URL of the file to process (image or PDF)
- `prompt_mode` (str): Processing mode (default: "prompt_grounding_ocr")
- `temperature` (float): Temperature parameter (default: 0.1)
- `top_p` (float): Top-p parameter (default: 1.0)
- `**kwargs`: Additional parameters

**Returns:** Dict with task information including `task_id`

**Example:**
```python
task = client.create_task(
    file_url="https://example.com/doc.pdf",
    prompt_mode="prompt_grounding_ocr"
)
print(f"Task ID: {task['task_id']}")
```

#### `get_task_result(task_id)`

Get task result by task ID.

**Parameters:**
- `task_id` (str): The task ID returned from `create_task`

**Returns:** Dict with task status and result

**Example:**
```python
result = client.get_task_result("task_123456")
print(result)
```

#### `wait_for_completion(task_id, max_wait_time=300, poll_interval=2)`

Poll task status until completion or timeout.

**Parameters:**
- `task_id` (str): The task ID to monitor
- `max_wait_time` (int): Maximum time to wait in seconds (default: 300)
- `poll_interval` (int): Time between polls in seconds (default: 2)

**Returns:** Final task result

**Raises:**
- `TimeoutError`: If task doesn't complete within max_wait_time
- `DotsOCRError`: If task fails

**Example:**
```python
task = client.create_task(file_url="...")
result = client.wait_for_completion(task["task_id"])
print(result)
```

#### `process_file(file_url, prompt_mode="prompt_grounding_ocr", temperature=0.1, top_p=1.0, max_wait_time=300, poll_interval=2, **kwargs)`

Convenience method to create task and wait for completion in one call.

**Parameters:**
- `file_url` (str): URL of the file to process
- `prompt_mode` (str): Processing mode
- `temperature` (float): Temperature parameter
- `top_p` (float): Top-p parameter
- `max_wait_time` (int): Maximum time to wait for completion
- `poll_interval` (int): Time between status checks
- `**kwargs`: Additional parameters

**Returns:** Final task result

**Example:**
```python
result = client.process_file("https://example.com/doc.pdf")
print(result)
```

### AsyncDotsOCRClient (Asynchronous)

The async client provides the same methods as the synchronous client, but all methods are async and must be awaited.

#### Usage with Context Manager

```python
async with AsyncDotsOCRClient(api_key="your-key") as client:
    result = await client.process_file("https://example.com/doc.pdf")
```

#### Usage with Existing Session

```python
import aiohttp

async with aiohttp.ClientSession() as session:
    client = AsyncDotsOCRClient(api_key="your-key", session=session)
    result = await client.process_file("https://example.com/doc.pdf")
```

## Examples

See the example files for more detailed usage:

- `example_sync.py` - Synchronous usage examples
- `example_async.py` - Asynchronous usage examples

### Run Examples

```bash
# Synchronous examples
python example_sync.py

# Asynchronous examples
python example_async.py
```

## Advanced Usage

### Custom Error Handling

```python
from dotsocr_client import DotsOCRClient, DotsOCRError, DotsOCRAPIError

client = DotsOCRClient(api_key="your-key")

try:
    result = client.process_file("https://example.com/doc.pdf")
except DotsOCRAPIError as e:
    print(f"API Error {e.status_code}: {e.message}")
except DotsOCRError as e:
    print(f"Error: {e}")
except TimeoutError as e:
    print(f"Timeout: {e}")
```

### Using Enums

```python
from dotsocr_client import DotsOCRClient, PromptMode

client = DotsOCRClient(api_key="your-key")

result = client.process_file(
    file_url="https://example.com/doc.pdf",
    prompt_mode=PromptMode.PROMPT_GROUNDING_OCR
)
```

### Processing Multiple Files Concurrently (Async)

```python
import asyncio
from dotsocr_client import AsyncDotsOCRClient

async def process_multiple_files(file_urls):
    async with AsyncDotsOCRClient(api_key="your-key") as client:
        tasks = [
            client.process_file(url)
            for url in file_urls
        ]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        return results

file_urls = [
    "https://example.com/doc1.pdf",
    "https://example.com/doc2.pdf",
    "https://example.com/doc3.pdf"
]

results = asyncio.run(process_multiple_files(file_urls))
```

### Custom Logging

```python
import logging
from dotsocr_client import DotsOCRClient

# Set logging level
logging.basicConfig(level=logging.DEBUG)

client = DotsOCRClient(api_key="your-key")
result = client.process_file("https://example.com/doc.pdf")
```

## Error Handling

The client raises the following exceptions:

- `DotsOCRError` - Base exception for all errors
- `DotsOCRAPIError` - API-related errors (HTTP errors, rate limits, etc.)
  - Includes `status_code`, `message`, and `response` attributes
- `ValueError` - Invalid input parameters
- `TimeoutError` - Task didn't complete within the specified time

## Configuration

### Environment Variables

Create a `.env` file:

```bash
DOTS_OCR_API_KEY=your-api-key-here
```

Load in your code:

```python
import os
from dotenv import load_dotenv
from dotsocr_client import DotsOCRClient

load_dotenv()
client = DotsOCRClient(api_key=os.getenv("DOTS_OCR_API_KEY"))
```

## API Parameters

### Prompt Modes

Currently supported:
- `prompt_grounding_ocr` - Standard OCR with grounding

### Temperature and Top-P

- `temperature` (0.0-1.0): Controls randomness. Lower = more deterministic
- `top_p` (0.0-1.0): Controls diversity via nucleus sampling

## Requirements

- Python 3.7+
- requests
- python-dotenv
- aiohttp (for async client)

## License

See LICENSE file for details.

## Support

For issues and questions:
- API documentation: https://302.ai
- Report bugs: Create an issue in this repository

## Changelog

### Version 1.0.0
- Initial release
- Synchronous and asynchronous clients
- Task creation, polling, and result retrieval
- Comprehensive error handling
- Examples and documentation
