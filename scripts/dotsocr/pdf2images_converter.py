#!/usr/bin/env python3
"""
PDF to Images Converter

PDF 파일을 입력받아서, PDF가 있는 경로에 PDF 이름과 동일한 디렉토리를 생성하고
각 페이지 당 하나의 이미지 파일(PNG)을 생성합니다.

Usage:
    python pdf2images_converter.py <pdf_path>
    python pdf2images_converter.py ../../dataset/downloads/suneung/수학영역_문제지.pdf

Requirements:
    pip install pdf2image pillow

System Requirements:
    - poppler-utils (Linux: apt-get install poppler-utils)
    - poppler (macOS: brew install poppler)
"""

import os
import sys
from pathlib import Path
from pdf2image import convert_from_path


def pdf_to_images(pdf_path: str, dpi: int = 200, fmt: str = "PNG") -> None:
    """
    PDF 파일을 이미지로 변환하여 저장합니다.

    Args:
        pdf_path: PDF 파일 경로
        dpi: 이미지 해상도 (기본값: 200)
        fmt: 이미지 포맷 (기본값: PNG)

    Raises:
        FileNotFoundError: PDF 파일이 존재하지 않을 때
        ValueError: PDF 파일이 아닐 때
    """
    pdf_path = Path(pdf_path).resolve()

    # PDF 파일 존재 여부 확인
    if not pdf_path.exists():
        raise FileNotFoundError(f"PDF 파일을 찾을 수 없습니다: {pdf_path}")

    # PDF 파일 확장자 확인
    if pdf_path.suffix.lower() != ".pdf":
        raise ValueError(f"PDF 파일이 아닙니다: {pdf_path}")

    # 출력 디렉토리 생성 (PDF 파일명과 동일)
    output_dir = pdf_path.parent / pdf_path.stem
    output_dir.mkdir(exist_ok=True)

    print(f"PDF 변환 시작: {pdf_path}")
    print(f"출력 디렉토리: {output_dir}")
    print(f"해상도: {dpi} DPI")

    try:
        # PDF를 이미지로 변환
        images = convert_from_path(str(pdf_path), dpi=dpi)
        total_pages = len(images)

        print(f"총 {total_pages} 페이지 변환 중...")

        # 각 페이지를 이미지 파일로 저장
        for i, image in enumerate(images, start=1):
            # page_0001.png, page_0002.png, ... 형식
            output_filename = f"page_{i:04d}.png"
            output_path = output_dir / output_filename

            image.save(str(output_path), fmt)
            print(f"  [{i}/{total_pages}] {output_filename} 저장 완료")

        print(f"\n✓ 변환 완료! 총 {total_pages}개 이미지 파일 생성")
        print(f"  저장 위치: {output_dir}")

    except Exception as e:
        print(f"\n✗ 변환 중 오류 발생: {e}", file=sys.stderr)
        raise


def main():
    if len(sys.argv) != 2:
        print("Usage: python pdf2images_converter.py <pdf_path>")
        print("Example: python pdf2images_converter.py ../../dataset/downloads/suneung/수학영역_문제지.pdf")
        sys.exit(1)

    pdf_path = sys.argv[1]

    try:
        pdf_to_images(pdf_path)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
