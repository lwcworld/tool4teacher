#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
pdf_ocr_async.py

PDF 파일을 입력받아서 각 페이지당 하나의 이미지로 분할하고,
비동기 방식으로 OCR 처리합니다.
출력된 OCR 결과는 이미지와 같은 디렉토리에 JSON으로 저장합니다.

사용 예:
    python pdf_ocr_async.py --pdf path/to/document.pdf
    python pdf_ocr_async.py --pdf path/to/document.pdf --dpi 300 --max-concurrent 5
"""

import argparse
import asyncio
import json
import random
import time
from pathlib import Path
from typing import List, Tuple, Optional

from pdf2images_converter import pdf_to_images
from dotsocr_client import DotsOCRClient


async def watch_prediction(client: DotsOCRClient, pred_id: str, image_path: str, *, interval: float = 1.5, timeout: float = 300.0):
    """
    예측 상태를 주기적으로 확인하며 완료될 때까지 대기합니다.
    """
    start = time.time()
    last_status = None

    while True:
        pred = await client.async_get_prediction(pred_id)
        status = getattr(pred, "status", None)
        elapsed = time.time() - start

        if status != last_status:
            print(f"[{Path(image_path).name}] status={status} elapsed={elapsed:.1f}s")
            last_status = status

        if status in ("succeeded", "failed", "canceled"):
            return pred

        if elapsed > timeout:
            raise TimeoutError(f"[{Path(image_path).name}] 타임아웃 초과 ({timeout}s)")

        await asyncio.sleep(interval)


def save_ocr_result(image_path: str, ocr_output: str) -> str:
    """
    OCR 결과를 JSON 파일로 저장합니다.
    """
    image_path = Path(image_path)
    json_path = image_path.with_suffix('.json')

    try:
        if isinstance(ocr_output, str):
            ocr_data = json.loads(ocr_output)
        else:
            ocr_data = ocr_output

        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(ocr_data, f, ensure_ascii=False, indent=2)

        print(f"[{image_path.name}] OCR 결과 저장: {json_path.name}")
        return str(json_path)

    except json.JSONDecodeError as e:
        print(f"[{image_path.name}] JSON 파싱 실패, 원본 저장: {e}")
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump({"raw_output": str(ocr_output)}, f, ensure_ascii=False, indent=2)
        return str(json_path)


async def ocr_one_image(
    client: DotsOCRClient,
    image_path: str,
    *,
    max_retries: int = 0,
    retry_initial_delay: float = 2.0,
    retry_backoff: float = 2.0
) -> Tuple[str, str, Optional[str]]:
    """
    하나의 이미지에 대해 OCR을 수행하고 즉시 저장합니다.
    실패/에러/타임아웃 시 지정 횟수만큼 재시도합니다.

    Returns:
        (image_path, status, json_path)
    """
    image_name = Path(image_path).name

    attempt = 0
    last_status: Optional[str] = None
    last_err: Optional[Exception] = None

    while attempt <= max_retries:
        attempt += 1
        pred = None
        print(f"[{image_name}] OCR 시도 {attempt}/{max_retries + 1}")

        try:
            # 1) 예측 생성
            pred = await client.async_create_prediction(
                image=image_path,
                top_p=1,
                prompt=(
                    "Please output the layout information from the PDF image, including each layout element's bbox, "
                    "its category, and the corresponding text content within the bbox.\n\n"
                    "1. Bbox format: [x1, y1, x2, y2]\n\n"
                    "2. Layout Categories: The possible categories are "
                    "['Caption', 'Footnote', 'Formula', 'List-item', 'Page-footer', 'Page-header', "
                    "'Picture', 'Section-header', 'Table', 'Text', 'Title'].\n\n"
                    "3. Text Extraction & Formatting Rules:\n"
                    "    - Picture: For the 'Picture' category, the text field should be omitted.\n"
                    "    - Formula: Format its text as LaTeX.\n"
                    "    - Table: Format its text as HTML.\n"
                    "    - All Others (Text, Title, etc.): Format their text as Markdown.\n\n"
                    "4. Constraints:\n"
                    "    - The output text must be the original text from the image, with no translation.\n"
                    "    - All layout elements must be sorted according to human reading order.\n\n"
                    "5. Final Output: The entire output must be a single JSON object."
                ),
                max_tokens=16384,
                temperature=0.1
            )
            print(f"[{image_name}] 예측 생성 완료: prediction_id={pred.id}")

            # 2) 완료 대기
            done = await watch_prediction(client, pred.id, image_path, interval=1.5, timeout=300.0)

            if done.status == "succeeded":
                print(f"[{image_name}] OCR 성공")
                json_path = save_ocr_result(image_path, done.output)
                return image_path, done.status, json_path

            # 실패/취소 등
            last_status = done.status
            print(f"[{image_name}] OCR 실패: status={done.status}")

        except TimeoutError as te:
            last_status = "timeout"
            last_err = te
            print(f"[{image_name}] 타임아웃: {te}")
            try:
                if pred and getattr(pred, "id", None):
                    await client.async_cancel_prediction(pred.id)
                    print(f"[{image_name}] 타임아웃으로 인한 예측 취소")
            except Exception as ce:
                print(f"[{image_name}] 예측 취소 중 에러: {ce}")

        except Exception as e:
            last_status = "error"
            last_err = e
            print(f"[{image_name}] 에러 발생: {e}")

        # 여기까지 왔다면 성공 못함 → 재시도 여부 판단
        if attempt <= max_retries:
            delay = retry_initial_delay * (retry_backoff ** (attempt - 1))
            delay += random.uniform(0, 0.5)  # 약간의 지터
            print(f"[{image_name}] {delay:.1f}s 후 재시도합니다...")
            await asyncio.sleep(delay)
        else:
            final_status = last_status or "failed"
            if last_err:
                print(f"[{image_name}] 최종 실패({final_status}): {last_err}")
            else:
                print(f"[{image_name}] 최종 실패({final_status})")
            return image_path, final_status, None


async def process_pdf_ocr(
    pdf_path: str,
    dpi: int = 200,
    max_concurrent: int = 3,
    *,
    max_retries: int = 0,
    retry_initial_delay: float = 2.0,
    retry_backoff: float = 2.0,
    skip_existing: bool = False
) -> List[Tuple[str, str, Optional[str]]]:
    """
    PDF를 이미지로 변환하고 비동기 OCR을 수행합니다.

    skip_existing=True이면, 같은 경로의 .json 결과가 이미 존재하는 이미지(page_XXXX.png)는
    OCR을 건너뜁니다.
    """
    # 1. PDF를 이미지로 변환
    print("="*60)
    print("1단계: PDF를 이미지로 변환")
    print("="*60)
    pdf_to_images(pdf_path, dpi=dpi)

    # 변환된 이미지 경로 찾기
    pdf_path = Path(pdf_path).resolve()
    output_dir = pdf_path.parent / pdf_path.stem
    image_paths = sorted([str(p) for p in output_dir.glob("page_*.png")])

    if not image_paths:
        raise FileNotFoundError(f"변환된 이미지를 찾을 수 없습니다: {output_dir}")

    print(f"\n총 {len(image_paths)}개 이미지 파일 발견\n")

    # 2. 스킵 처리
    results: List[Tuple[str, str, Optional[str]]] = []
    if skip_existing:
        remaining: List[str] = []
        skipped_count = 0
        for p in image_paths:
            jp = Path(p).with_suffix(".json")
            if jp.exists():
                print(f"[{Path(p).name}] 기존 JSON 존재 -> 건너뜀")
                results.append((p, "skipped_existing", str(jp)))
                skipped_count += 1
            else:
                remaining.append(p)
        image_paths = remaining
        print(f"\n스킵된 이미지: {skipped_count}개")
        print(f"OCR 대상 이미지: {len(image_paths)}개\n")

    # 3. DotsOCR 클라이언트 생성
    client = DotsOCRClient()

    # 4. 비동기 OCR 처리
    print("="*60)
    print("2단계: 비동기 OCR 처리")
    print("="*60)
    print(f"총 OCR 대상: {len(image_paths)}개")
    print(f"최대 동시 처리 수: {max_concurrent}")
    print(f"실패 시 재시도: 최대 {max_retries}회, 초기지연 {retry_initial_delay}s, 백오프 {retry_backoff}x\n")

    semaphore = asyncio.Semaphore(max_concurrent)

    async def ocr_with_limit(image_path: str):
        async with semaphore:
            return await ocr_one_image(
                client,
                image_path,
                max_retries=max_retries,
                retry_initial_delay=retry_initial_delay,
                retry_backoff=retry_backoff
            )

    tasks = [ocr_with_limit(path) for path in image_paths]
    if tasks:
        ocr_results = await asyncio.gather(*tasks, return_exceptions=True)
        # 결과 합치기 (스킵된 것 + 새로 처리한 것)
        for r in ocr_results:
            if isinstance(r, Exception):
                print(f"[에러] {r}")
                # 어떤 이미지였는지 식별이 어려우므로 요약에서만 반영
                continue
            results.append(r)

    # 5. 결과 집계
    saved_files = []
    succeeded = 0
    skipped = 0
    failed = 0
    for item in results:
        if isinstance(item, Exception):
            failed += 1
            continue
        image_path, status, json_path = item
        if status == "succeeded" and json_path:
            saved_files.append(json_path)
            succeeded += 1
        elif status == "skipped_existing":
            skipped += 1
        else:
            failed += 1

    # 6. 요약 출력
    total_images = len([*output_dir.glob("page_*.png")])
    print(f"\n{'='*60}")
    print("완료 요약")
    print("="*60)
    print(f"총 이미지: {total_images}개")
    print(f"OCR 성공: {succeeded}개")
    print(f"스킵(기존 JSON 존재): {skipped}개")
    print(f"OCR 실패: {failed}개")
    print(f"저장 위치: {output_dir}")
    print("="*60)

    return results


def main():
    parser = argparse.ArgumentParser(
        description="PDF를 페이지별 이미지로 변환하고 비동기 OCR을 수행합니다."
    )
    parser.add_argument("--pdf", required=True, help="처리할 PDF 파일 경로")
    parser.add_argument("--dpi", type=int, default=100, help="이미지 해상도 (기본값: 200)")
    parser.add_argument("--max-concurrent", type=int, default=3, help="최대 동시 처리 수 (기본값: 3)")

    # 재시도 옵션
    parser.add_argument("--max-retries", type=int, default=3, help="OCR 실패 시 재시도 횟수 (기본값: 0 = 재시도 없음)")
    parser.add_argument("--retry-initial-delay", type=float, default=2.0, help="재시도 초기 지연(초) (기본값: 2.0)")
    parser.add_argument("--retry-backoff", type=float, default=2.0, help="재시도 지수 백오프 배수 (기본값: 2.0)")

    # 기존 결과 스킵 옵션
    parser.add_argument("--skip-existing", type=bool, default=False, help="이미 JSON 결과가 있는 이미지는 건너뜁니다.")

    args = parser.parse_args()

    asyncio.run(process_pdf_ocr(
        pdf_path=args.pdf,
        dpi=args.dpi,
        max_concurrent=args.max_concurrent,
        max_retries=args.max_retries,
        retry_initial_delay=args.retry_initial_delay,
        retry_backoff=args.retry_backoff,
        skip_existing=args.skip_existing
    ))


if __name__ == "__main__":
    main()
