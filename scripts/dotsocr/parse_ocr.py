#!/usr/bin/env python3
"""
OCR 결과 JSON을 사용하여 페이지 이미지를 시각화합니다.
각 PNG/JSON 쌍에 대해 page_{num}_parse.png를 생성하고,
바운딩 박스를 80% 투명도로 채우고 카테고리 라벨을 표시합니다.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Iterable, List, Mapping, Sequence

from PIL import Image, ImageDraw, ImageFont


SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_OUTPUT_DIR = (SCRIPT_DIR / "../../dataset/downloads/suneung/수학영역_문제지").resolve()

# 고정 색상 팔레트 (카테고리 수가 늘어나면 순환 사용)
PALETTE: Sequence[tuple[int, int, int]] = (
    (230, 57, 70),   # 붉은색
    (29, 53, 87),    # 남색
    (69, 123, 157),  # 청회색
    (168, 218, 220), # 연한 청록
    (42, 157, 143),  # 청록
    (38, 70, 83),    # 진한 청록
    (233, 196, 106), # 노랑
    (244, 162, 97),  # 주황
    (231, 111, 81),  # 살구
    (87, 117, 144),  # 회청
    (199, 0, 57),    # 다홍
)
FILL_ALPHA = int(255 * 0.3)
TEXT_PADDING = 2


def iter_annotations(data: object) -> Iterable[Mapping[str, object]]:
    """
    OCR JSON에서 어노테이션 리스트를 추출합니다.
    리스트 형태 또는 dict 내부 리스트 형태 모두 지원합니다.
    """
    if isinstance(data, list):
        yield from (item for item in data if isinstance(item, Mapping))
    elif isinstance(data, Mapping):
        for key in ("items", "predictions", "outputs", "elements", "data"):
            maybe_items = data.get(key)
            if isinstance(maybe_items, list):
                yield from (item for item in maybe_items if isinstance(item, Mapping))
                return
    else:
        return


def color_for_index(index: int) -> tuple[int, int, int]:
    return PALETTE[index % len(PALETTE)]


def draw_annotations(
    base_image: Image.Image,
    annotations: Iterable[Mapping[str, object]],
) -> Image.Image:
    overlay = Image.new("RGBA", base_image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    font = ImageFont.load_default()

    for idx, ann in enumerate(annotations):
        bbox = ann.get("bbox")
        if not (isinstance(bbox, Sequence) and len(bbox) == 4):
            continue

        try:
            x1, y1, x2, y2 = (int(float(v)) for v in bbox)
        except (TypeError, ValueError):
            continue

        x1 = max(0, x1)
        y1 = max(0, y1)
        x2 = min(base_image.width - 1, max(x1 + 1, x2))
        y2 = min(base_image.height - 1, max(y1 + 1, y2))

        category = str(ann.get("category", "Unknown"))
        color = color_for_index(idx)

        draw.rectangle(
            [(x1, y1), (x2, y2)],
            fill=(*color, FILL_ALPHA),
            outline=(*color, 255),
            width=2,
        )

        label = f"{idx + 1}. {category}"
        text_bbox = draw.textbbox((0, 0), label, font=font)
        tw = text_bbox[2] - text_bbox[0]
        th = text_bbox[3] - text_bbox[1]
        tx = min(max(x1 + TEXT_PADDING, 0), base_image.width - tw - 1)
        ty = min(max(y1 + TEXT_PADDING, 0), base_image.height - th - 1)
        bg_rect = [
            tx - TEXT_PADDING,
            ty - TEXT_PADDING,
            tx + tw + TEXT_PADDING,
            ty + th + TEXT_PADDING,
        ]
        draw.rectangle(bg_rect, fill=(*color, 230))
        draw.text((tx, ty), label, fill=(255, 255, 255, 255), font=font)

    return Image.alpha_composite(base_image, overlay)


def process_directory(target_dir: Path) -> List[Path]:
    if not target_dir.exists():
        raise FileNotFoundError(f"경로를 찾을 수 없습니다: {target_dir}")
    if not target_dir.is_dir():
        raise NotADirectoryError(f"디렉토리가 아닙니다: {target_dir}")

    generated: List[Path] = []

    for png_path in sorted(target_dir.glob("page_*.png")):
        if png_path.stem.endswith("_parse"):
            continue
        json_path = png_path.with_suffix(".json")
        if not json_path.exists():
            print(f"[skip] JSON이 없어 건너뜁니다: {json_path.name}", file=sys.stderr)
            continue

        try:
            with open(json_path, "r", encoding="utf-8") as f:
                data = json.load(f)
        except json.JSONDecodeError as exc:
            print(f"[error] JSON 파싱 실패 ({json_path.name}): {exc}", file=sys.stderr)
            continue

        annotations = list(iter_annotations(data))
        if not annotations:
            print(f"[warn] 유효한 어노테이션이 없습니다: {json_path.name}", file=sys.stderr)
            continue

        with Image.open(png_path) as img:
            composite = draw_annotations(img.convert("RGBA"), annotations)

        output_path = png_path.with_name(f"{png_path.stem}_parse.png")
        composite.convert("RGBA").save(output_path)
        generated.append(output_path)
        print(f"[ok] {output_path.name} 생성 완료")

    if not generated:
        print("[info] 처리된 파일이 없습니다. PNG/JSON 쌍을 확인하세요.", file=sys.stderr)

    return generated


def main() -> int:
    parser = argparse.ArgumentParser(
        description="OCR JSON을 사용해 page_{num}_parse.png 이미지를 생성합니다."
    )
    parser.add_argument(
        "--input-dir",
        default=str(DEFAULT_OUTPUT_DIR),
        help="PNG/JSON 파일이 위치한 디렉토리 (기본값: 수학영역_문제지 출력 경로)",
    )
    args = parser.parse_args()

    target_dir = Path(args.input_dir).expanduser().resolve()

    try:
        process_directory(target_dir)
    except Exception as exc:  # noqa: BLE001
        print(f"[fatal] 처리 중 오류: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
