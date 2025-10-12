"""
DotsOCR API Client
A Python wrapper for the 302.ai DotsOCR API.
"""

import json
import time
import logging
from typing import Optional, Dict, Any, Union
from enum import Enum
import http.client
from urllib.parse import urlencode

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class PromptMode(Enum):
    """Available prompt modes for DotsOCR"""
    PROMPT_GROUNDING_OCR = "prompt_grounding_ocr"
    # Add other modes as needed


class TaskStatus(Enum):
    """Task status enum"""
    PENDING = "pending"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"


class DotsOCRError(Exception):
    """Base exception for DotsOCR API errors"""
    pass


class DotsOCRAPIError(DotsOCRError):
    """Exception for API-related errors"""
    def __init__(self, status_code: int, message: str, response: Optional[str] = None):
        self.status_code = status_code
        self.message = message
        self.response = response
        super().__init__(f"API Error {status_code}: {message}")


class DotsOCRClient:
    """
    Synchronous client for DotsOCR API

    Example:
        client = DotsOCRClient(api_key="your-api-key")
        task = client.create_task(
            file_url="https://example.com/document.pdf",
            prompt_mode="prompt_grounding_ocr"
        )
        result = client.get_task_result(task["task_id"])
    """

    BASE_HOST = "api.302.ai"
    BASE_PATH = "/302/v2/dots_ocr"

    def __init__(
        self,
        api_key: str,
        timeout: int = 30,
        use_https: bool = True
    ):
        """
        Initialize DotsOCR client

        Args:
            api_key: Your 302.ai API key
            timeout: Request timeout in seconds (default: 30)
            use_https: Use HTTPS connection (default: True)
        """
        if not api_key:
            raise ValueError("API key is required")

        self.api_key = api_key
        self.timeout = timeout
        self.use_https = use_https
        self._connection_class = (
            http.client.HTTPSConnection if use_https
            else http.client.HTTPConnection
        )

    def _make_request(
        self,
        method: str,
        endpoint: str,
        payload: Union[str, Dict] = "",
        params: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Make HTTP request to API

        Args:
            method: HTTP method (GET, POST, etc.)
            endpoint: API endpoint path
            payload: Request payload (dict or string)
            params: URL query parameters

        Returns:
            Parsed JSON response

        Raises:
            DotsOCRAPIError: If request fails
        """
        conn = None
        try:
            conn = self._connection_class(self.BASE_HOST, timeout=self.timeout)

            # Prepare headers
            headers = {
                'Authorization': f'Bearer {self.api_key}'
            }

            # Prepare payload and content type
            if isinstance(payload, dict):
                payload = json.dumps(payload)
                headers['Content-Type'] = 'application/json'

            # Build URL with query parameters
            url = f"{self.BASE_PATH}{endpoint}"
            if params:
                url = f"{url}?{urlencode(params)}"

            logger.debug(f"Making {method} request to {url}")

            # Make request
            conn.request(method, url, payload, headers)
            response = conn.getresponse()
            data = response.read()

            # Parse response
            response_text = data.decode("utf-8")
            logger.debug(f"Response status: {response.status}")
            logger.debug(f"Response body: {response_text[:500]}")

            # Check for errors
            if response.status >= 400:
                error_msg = f"Request failed with status {response.status}"
                try:
                    error_data = json.loads(response_text)
                    error_msg = error_data.get('message', error_msg)
                except json.JSONDecodeError:
                    error_msg = response_text

                raise DotsOCRAPIError(
                    status_code=response.status,
                    message=error_msg,
                    response=response_text
                )

            # Parse JSON response
            try:
                return json.loads(response_text)
            except json.JSONDecodeError as e:
                raise DotsOCRError(f"Failed to parse JSON response: {e}")

        except http.client.HTTPException as e:
            raise DotsOCRError(f"HTTP connection error: {e}")
        except Exception as e:
            if isinstance(e, DotsOCRError):
                raise
            raise DotsOCRError(f"Unexpected error: {e}")
        finally:
            if conn:
                conn.close()

    def create_task(
        self,
        file_url: str,
        prompt_mode: Union[str, PromptMode] = PromptMode.PROMPT_GROUNDING_OCR,
        temperature: float = 0.1,
        top_p: float = 1.0,
        **kwargs
    ) -> Dict[str, Any]:
        """
        Create a new OCR task

        Args:
            file_url: URL of the file to process (image or PDF)
            prompt_mode: Processing mode (default: prompt_grounding_ocr)
            temperature: Temperature parameter (default: 0.1)
            top_p: Top-p parameter (default: 1.0)
            **kwargs: Additional parameters to include in the request

        Returns:
            API response with task information including task_id

        Example:
            task = client.create_task(
                file_url="https://example.com/document.pdf",
                prompt_mode="prompt_grounding_ocr"
            )
            print(f"Task ID: {task['task_id']}")
        """
        if isinstance(prompt_mode, PromptMode):
            prompt_mode = prompt_mode.value

        payload = {
            "file_url": file_url,
            "prompt_mode": prompt_mode,
            "temperature": temperature,
            "top_p": top_p,
            **kwargs
        }

        logger.info(f"Creating OCR task for file: {file_url}")
        response = self._make_request("POST", "/task", payload=payload)

        if "task_id" in response:
            logger.info(f"Task created successfully: {response['task_id']}")

        return response

    def get_task_result(
        self,
        task_id: str
    ) -> Dict[str, Any]:
        """
        Get task result by task ID

        Args:
            task_id: The task ID returned from create_task

        Returns:
            API response with task status and result

        Example:
            result = client.get_task_result("task_123456")
            print(result)
        """
        if not task_id:
            raise ValueError("task_id is required")

        logger.info(f"Fetching result for task: {task_id}")
        response = self._make_request(
            "GET",
            "/task",
            params={"task_id": task_id}
        )

        return response

    def wait_for_completion(
        self,
        task_id: str,
        max_wait_time: int = 300,
        poll_interval: int = 2
    ) -> Dict[str, Any]:
        """
        Poll task status until completion or timeout

        Args:
            task_id: The task ID to monitor
            max_wait_time: Maximum time to wait in seconds (default: 300)
            poll_interval: Time between polls in seconds (default: 2)

        Returns:
            Final task result

        Raises:
            TimeoutError: If task doesn't complete within max_wait_time
            DotsOCRError: If task fails

        Example:
            task = client.create_task(file_url="...")
            result = client.wait_for_completion(task["task_id"])
            print(result)
        """
        start_time = time.time()

        logger.info(f"Waiting for task {task_id} to complete...")

        while True:
            elapsed = time.time() - start_time

            if elapsed > max_wait_time:
                raise TimeoutError(
                    f"Task {task_id} did not complete within {max_wait_time} seconds"
                )

            result = self.get_task_result(task_id)
            status = result.get("status", "").lower()

            logger.debug(f"Task {task_id} status: {status}")

            # Check if task is completed
            if status == TaskStatus.COMPLETED.value or "result" in result:
                logger.info(f"Task {task_id} completed successfully")
                return result

            # Check if task failed
            if status == TaskStatus.FAILED.value or "error" in result:
                error_msg = result.get("error", "Task failed")
                raise DotsOCRError(f"Task {task_id} failed: {error_msg}")

            # Wait before next poll
            time.sleep(poll_interval)

    def process_file(
        self,
        file_url: str,
        prompt_mode: Union[str, PromptMode] = PromptMode.PROMPT_GROUNDING_OCR,
        temperature: float = 0.1,
        top_p: float = 1.0,
        max_wait_time: int = 300,
        poll_interval: int = 2,
        **kwargs
    ) -> Dict[str, Any]:
        """
        Convenience method to create task and wait for completion

        Args:
            file_url: URL of the file to process
            prompt_mode: Processing mode
            temperature: Temperature parameter
            top_p: Top-p parameter
            max_wait_time: Maximum time to wait for completion
            poll_interval: Time between status checks
            **kwargs: Additional parameters for task creation

        Returns:
            Final task result

        Example:
            result = client.process_file("https://example.com/doc.pdf")
            print(result)
        """
        # Create task
        task = self.create_task(
            file_url=file_url,
            prompt_mode=prompt_mode,
            temperature=temperature,
            top_p=top_p,
            **kwargs
        )

        task_id = task.get("task_id")
        if not task_id:
            raise DotsOCRError("No task_id in response")

        # Wait for completion
        return self.wait_for_completion(
            task_id=task_id,
            max_wait_time=max_wait_time,
            poll_interval=poll_interval
        )


class AsyncDotsOCRClient:
    """
    Asynchronous client for DotsOCR API

    Requires: aiohttp

    Example:
        async with AsyncDotsOCRClient(api_key="your-api-key") as client:
            task = await client.create_task(file_url="...")
            result = await client.wait_for_completion(task["task_id"])
    """

    BASE_URL = "https://api.302.ai/302/v2/dots_ocr"

    def __init__(
        self,
        api_key: str,
        timeout: int = 30,
        session=None
    ):
        """
        Initialize async DotsOCR client

        Args:
            api_key: Your 302.ai API key
            timeout: Request timeout in seconds
            session: Optional aiohttp.ClientSession
        """
        if not api_key:
            raise ValueError("API key is required")

        self.api_key = api_key
        self.timeout = timeout
        self._session = session
        self._owned_session = session is None

    async def __aenter__(self):
        """Async context manager entry"""
        if self._owned_session:
            import aiohttp
            self._session = aiohttp.ClientSession()
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit"""
        if self._owned_session and self._session:
            await self._session.close()

    async def _make_request(
        self,
        method: str,
        endpoint: str,
        payload: Optional[Dict] = None,
        params: Optional[Dict] = None
    ) -> Dict[str, Any]:
        """Make async HTTP request"""
        if not self._session:
            raise RuntimeError("Client must be used as context manager or with session")

        import aiohttp

        url = f"{self.BASE_URL}{endpoint}"
        headers = {
            'Authorization': f'Bearer {self.api_key}',
            'Content-Type': 'application/json'
        }

        timeout = aiohttp.ClientTimeout(total=self.timeout)

        try:
            async with self._session.request(
                method=method,
                url=url,
                json=payload,
                params=params,
                headers=headers,
                timeout=timeout
            ) as response:
                response_text = await response.text()

                if response.status >= 400:
                    error_msg = f"Request failed with status {response.status}"
                    try:
                        error_data = json.loads(response_text)
                        error_msg = error_data.get('message', error_msg)
                    except json.JSONDecodeError:
                        error_msg = response_text

                    raise DotsOCRAPIError(
                        status_code=response.status,
                        message=error_msg,
                        response=response_text
                    )

                return json.loads(response_text)

        except aiohttp.ClientError as e:
            raise DotsOCRError(f"HTTP client error: {e}")

    async def create_task(
        self,
        file_url: str,
        prompt_mode: Union[str, PromptMode] = PromptMode.PROMPT_GROUNDING_OCR,
        temperature: float = 0.1,
        top_p: float = 1.0,
        **kwargs
    ) -> Dict[str, Any]:
        """Create OCR task (async)"""
        if isinstance(prompt_mode, PromptMode):
            prompt_mode = prompt_mode.value

        payload = {
            "file_url": file_url,
            "prompt_mode": prompt_mode,
            "temperature": temperature,
            "top_p": top_p,
            **kwargs
        }

        logger.info(f"Creating OCR task for file: {file_url}")
        response = await self._make_request("POST", "/task", payload=payload)

        if "task_id" in response:
            logger.info(f"Task created successfully: {response['task_id']}")

        return response

    async def get_task_result(self, task_id: str) -> Dict[str, Any]:
        """Get task result (async)"""
        if not task_id:
            raise ValueError("task_id is required")

        logger.info(f"Fetching result for task: {task_id}")
        return await self._make_request(
            "GET",
            "/task",
            params={"task_id": task_id}
        )

    async def wait_for_completion(
        self,
        task_id: str,
        max_wait_time: int = 300,
        poll_interval: int = 2
    ) -> Dict[str, Any]:
        """Wait for task completion (async)"""
        import asyncio

        start_time = time.time()
        logger.info(f"Waiting for task {task_id} to complete...")

        while True:
            elapsed = time.time() - start_time

            if elapsed > max_wait_time:
                raise TimeoutError(
                    f"Task {task_id} did not complete within {max_wait_time} seconds"
                )

            result = await self.get_task_result(task_id)
            status = result.get("status", "").lower()

            logger.debug(f"Task {task_id} status: {status}")

            if status == TaskStatus.COMPLETED.value or "result" in result:
                logger.info(f"Task {task_id} completed successfully")
                return result

            if status == TaskStatus.FAILED.value or "error" in result:
                error_msg = result.get("error", "Task failed")
                raise DotsOCRError(f"Task {task_id} failed: {error_msg}")

            await asyncio.sleep(poll_interval)

    async def process_file(
        self,
        file_url: str,
        prompt_mode: Union[str, PromptMode] = PromptMode.PROMPT_GROUNDING_OCR,
        temperature: float = 0.1,
        top_p: float = 1.0,
        max_wait_time: int = 300,
        poll_interval: int = 2,
        **kwargs
    ) -> Dict[str, Any]:
        """Create task and wait for completion (async)"""
        task = await self.create_task(
            file_url=file_url,
            prompt_mode=prompt_mode,
            temperature=temperature,
            top_p=top_p,
            **kwargs
        )

        task_id = task.get("task_id")
        if not task_id:
            raise DotsOCRError("No task_id in response")

        return await self.wait_for_completion(
            task_id=task_id,
            max_wait_time=max_wait_time,
            poll_interval=poll_interval
        )
