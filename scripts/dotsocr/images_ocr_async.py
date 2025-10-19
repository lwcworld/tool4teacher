import asyncio
import time
from dotsocr_client import DotsOCRClient

IMAGES = [
    "../../dataset/downloads/suneung/수학영역_문제지/page_0001.png",
    "../../dataset/downloads/suneung/수학영역_문제지/page_0004.png",
    "../../dataset/downloads/suneung/수학영역_문제지/page_0005.png",
]

async def watch_prediction(client: DotsOCRClient, pred_id: str, *, interval: float = 1.5, timeout: float = 300.0):
    """
    상태/로그를 주기적으로 프린트하며 완료될 때까지 기다립니다.
    """
    start = time.time()
    last_status = None
    while True:
        pred = await client.async_get_prediction(pred_id)
        status = getattr(pred, "status", None)
        logs = getattr(pred, "logs", "") or ""
        elapsed = time.time() - start

        # 상태가 바뀌었을 때만(혹은 주기적으로) 출력
        if status != last_status:
            print(f"[{pred_id}] status={status} elapsed={elapsed:.1f}s")
            last_status = status

        # 로그 마지막 줄(있으면)
        if logs:
            last_line = logs.strip().splitlines()[-1]
            print(f"[{pred_id}] log: {last_line}")

        if status in ("succeeded", "failed", "canceled"):
            return pred

        if elapsed > timeout:
            raise TimeoutError(f"[{pred_id}] wait timeout exceeded ({timeout}s)")

        await asyncio.sleep(interval)

async def predict_one(client: DotsOCRClient, image_path: str):
    # 예측 생성 - Replicate input 파라미터들을 직접 지정
    pred = await client.async_create_prediction(
        image=image_path,
        top_p=1,
        prompt="Please output the layout information from the PDF image, including each layout element's bbox, its category, and the corresponding text content within the bbox.\n\n1. Bbox format: [x1, y1, x2, y2]\n\n2. Layout Categories: The possible categories are ['Caption', 'Footnote', 'Formula', 'List-item', 'Page-footer', 'Page-header', 'Picture', 'Section-header', 'Table', 'Text', 'Title'].\n\n3. Text Extraction & Formatting Rules:\n    - Picture: For the 'Picture' category, the text field should be omitted.\n    - Formula: Format its text as LaTeX.\n    - Table: Format its text as HTML.\n    - All Others (Text, Title, etc.): Format their text as Markdown.\n\n4. Constraints:\n    - The output text must be the original text from the image, with no translation.\n    - All layout elements must be sorted according to human reading order.\n\n5. Final Output: The entire output must be a single JSON object.",
        max_tokens=16384,
        temperature=0.1
    )
    print(f"[{image_path}] created prediction_id={pred.id}")

    try:
        done = await watch_prediction(client, pred.id, interval=1.5, timeout=300.0)
        print(f"[{image_path}] FINAL status={done.status}")
        # 필요 시 output 길이가 길 수 있으니 요약/일부만 출력
        print(f"[{image_path}] output_preview={str(done.output)[:300]}...")
        return image_path, done.status, done.output
    except TimeoutError as te:
        # 오래 걸리면 취소 시도
        try:
            await client.async_cancel_prediction(pred.id)
            print(f"[{image_path}] canceled due to timeout")
        except Exception as ce:
            print(f"[{image_path}] cancel error: {ce}")
        return image_path, "timeout", None
    except Exception as e:
        print(f"[{image_path}] error: {e}")
        return image_path, "error", None

async def main():
    client = DotsOCRClient()  # REPLICATE_API_TOKEN 환경변수 사용
    tasks = [predict_one(client, path) for path in IMAGES]
    results = await asyncio.gather(*tasks, return_exceptions=True)

    print("\n=== SUMMARY ===")
    for r in results:
        if isinstance(r, Exception):
            print("에러(상위):", r)
        else:
            url, status, output = r
            print(url, status, "(output omitted)" if output is not None else output)

if __name__ == "__main__":
    asyncio.run(main())
