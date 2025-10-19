#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
dotsocr_client.py

Replicate 의 sljeff/dots.ocr 모델을 간편하게 호출하기 위한 클라이언트 모듈.
- 환경변수 REPLICATE_API_TOKEN 을 사용하여 인증
- 이미지 입력: URL / 로컬 파일 경로 / data URI 모두 지원
- 동기: run(), create_prediction(), wait(), cancel()
- 비동기: async_create_prediction(), async_wait(), async_cancel()
- 계정 점검: check_account()
- CLI 지원

사용 예)
    # 단순 실행(replicate.run)
    python dotsocr_client.py run \
        --image https://replicate.delivery/pbxt/NWlQUUIAB0pn2n7niyz70GvU7lWZzadscMX9UNSCGnDv7IZI/docling-test-2_page-0020.jpg

    # 예측 객체 생성(predictions.create) 후 완료까지 대기
    python dotsocr_client.py create \
        --image ./path/to/my/image.png \
        --wait

    # 웹훅 지정하여 비동기 처리(폴링 불필요)
    python dotsocr_client.py create \
        --image https://.../image.jpg \
        --webhook https://my.app/webhooks/replicate \
        --webhook-events completed

    # 계정 토큰 점검
    python dotsocr_client.py account
"""

from __future__ import annotations

import argparse
import asyncio
import base64
import json
import os
import sys
import time
from pathlib import Path
from typing import Any, Dict, Iterable, Optional, Tuple, Union

try:
    import replicate
except ImportError as e:
    raise SystemExit(
        "replicate 라이브러리가 설치되어 있지 않습니다.\n"
        "설치: pip install replicate"
    ) from e

try:
    import requests  # 계정 점검용 (선택)
except Exception:
    requests = None  # 없어도 클라이언트 동작에는 지장 없음


# ----- 모델 고정 값 -----
# 모델 버전은 메뉴얼/예시에서 제공된 해시를 그대로 사용합니다.
MODEL_VERSION = "sljeff/dots.ocr:214a4fc47a5e8254ae83362a34271feeb53c5e61d9bc8aadcf96a5d8717be4d6"

# ----- 타입 -----
ImageLike = Union[str, Path]  # URL, 로컬 경로, data URI 문자열 모두 허용


def _is_url(s: str) -> bool:
    return s.startswith("http://") or s.startswith("https://")


def _is_data_uri(s: str) -> bool:
    return s.startswith("data:")


def _resolve_image_arg(image: ImageLike) -> Union[str, Any]:
    """
    이미지 인자를 Replicate 입력에 맞게 변환.
    - URL 문자열: 그대로 반환
    - data URI 문자열: 그대로 반환
    - 로컬 경로: 파일 객체(rb)로 열어서 반환 (업로드는 replicate 클라이언트가 처리)
    """
    if isinstance(image, Path):
        if not image.exists():
            raise FileNotFoundError(f"이미지 파일이 존재하지 않습니다: {image}")
        return open(image, "rb")

    if not isinstance(image, str):
        raise TypeError("image 인자는 str 또는 pathlib.Path 여야 합니다.")

    if _is_url(image) or _is_data_uri(image):
        return image

    # 문자열이지만 URL/dataURI가 아니면 로컬 경로로 간주
    p = Path(image)
    if not p.exists():
        raise FileNotFoundError(f"이미지 파일이 존재하지 않습니다: {image}")
    return open(p, "rb")


def make_data_uri_from_file(file_path: Union[str, Path], mime: str = "application/octet-stream") -> str:
    """
    (옵션) 로컬 파일을 data URI로 변환 (1MB 이하 권장)
    """
    p = Path(file_path)
    if not p.exists():
        raise FileNotFoundError(f"파일이 존재하지 않습니다: {file_path}")
    b64 = base64.b64encode(p.read_bytes()).decode("utf-8")
    return f"data:{mime};base64,{b64}"


class DotsOCRClient:
    """
    sljeff/dots.ocr 전용 클라이언트
    """

    def __init__(self, api_token: Optional[str] = None):
        token = api_token or os.getenv("REPLICATE_API_TOKEN")
        if not token:
            raise EnvironmentError(
                "REPLICATE_API_TOKEN 이 설정되어 있지 않습니다. "
                "export REPLICATE_API_TOKEN=... 혹은 .env 사용을 확인하세요."
            )
        # replicate.run 을 전역으로 바로 써도 되지만, 명시적으로 client를 유지합니다.
        self.client = replicate.Client(api_token=token)

    # ---------- 단순 실행 (출력만 반환) ----------
    def run(self, image: ImageLike, **extra_inputs: Any) -> Any:
        """
        replicate.run 사용. 출력 결과만 바로 반환.
        """
        img = _resolve_image_arg(image)
        try:
            _input = {"image": img}
            if extra_inputs:
                _input.update(extra_inputs)

            return replicate.run(MODEL_VERSION, input=_input)
        finally:
            # 파일 객체를 열었으면 닫아줍니다 (URL/dataURI는 str이므로 pass).
            if hasattr(img, "close"):
                try:
                    img.close()  # type: ignore[attr-defined]
                except Exception:
                    pass

    # ---------- 예측 객체 생성/관리 ----------
    def create_prediction(
        self,
        image: ImageLike,
        webhook: Optional[str] = None,
        webhook_events_filter: Optional[Iterable[str]] = None,
        **extra_inputs: Any,
    ):
        """
        predictions.create 사용. Prediction 객체를 반환(즉시).
        """
        img = _resolve_image_arg(image)
        try:
            payload: Dict[str, Any] = {
                "version": MODEL_VERSION.split(":")[1],  # 해시만 넘길 수도 있고,
                # Replicate Python 클라에서 owner/name:hash 형태의 version도 허용됩니다.
                # 안전하게 해시만 사용.
                "input": {"image": img, **(extra_inputs or {})},
            }
            if webhook:
                payload["webhook"] = webhook
            if webhook_events_filter:
                payload["webhook_events_filter"] = list(webhook_events_filter)

            return self.client.predictions.create(**payload)
        finally:
            if hasattr(img, "close"):
                try:
                    img.close()
                except Exception:
                    pass

    def get_prediction(self, prediction_id: str):
        return self.client.predictions.get(prediction_id)

    def cancel_prediction(self, prediction_id: str):
        pred = self.get_prediction(prediction_id)
        return pred.cancel()

    def wait(
        self,
        prediction_id: str,
        *,
        interval: float = 1.5,
        timeout: Optional[float] = 300.0,
        stop_on: Tuple[str, ...] = ("succeeded", "failed", "canceled"),
        echo_logs: bool = False,
    ):
        """
        예측 완료까지 폴링.
        """
        start = time.time()
        while True:
            pred = self.get_prediction(prediction_id)
            status = getattr(pred, "status", None)
            if echo_logs:
                logs = getattr(pred, "logs", None)
                if logs:
                    print(logs, flush=True)
            if status in stop_on:
                return pred
            if timeout is not None and (time.time() - start) > timeout:
                raise TimeoutError(f"예측 대기 시간이 초과되었습니다 (timeout={timeout}s). 현재 상태: {status}")
            time.sleep(interval)

    # ---------- 비동기 API ----------
    async def async_create_prediction(
        self,
        image: ImageLike,
        webhook: Optional[str] = None,
        webhook_events_filter: Optional[Iterable[str]] = None,
        **extra_inputs: Any,
    ):
        """
        predictions.async_create 사용. Prediction 객체를 반환(즉시).
        """
        img = _resolve_image_arg(image)
        try:
            payload: Dict[str, Any] = {
                "version": MODEL_VERSION.split(":")[1],
                "input": {"image": img, **(extra_inputs or {})},
            }
            if webhook:
                payload["webhook"] = webhook
            if webhook_events_filter:
                payload["webhook_events_filter"] = list(webhook_events_filter)

            return await self.client.predictions.async_create(**payload)
        finally:
            if hasattr(img, "close"):
                try:
                    img.close()
                except Exception:
                    pass

    async def async_get_prediction(self, prediction_id: str):
        return await self.client.predictions.async_get(prediction_id)

    async def async_cancel_prediction(self, prediction_id: str):
        pred = await self.async_get_prediction(prediction_id)
        return await pred.async_cancel()

    async def async_wait(
        self,
        prediction_id: str,
        *,
        interval: float = 1.5,
        timeout: Optional[float] = 300.0,
        stop_on: Tuple[str, ...] = ("succeeded", "failed", "canceled"),
        echo_logs: bool = False,
    ):
        """
        비동기 폴링.
        """
        start = time.time()
        while True:
            pred = await self.async_get_prediction(prediction_id)
            status = getattr(pred, "status", None)
            if echo_logs:
                logs = getattr(pred, "logs", None)
                if logs:
                    print(logs, flush=True)
            if status in stop_on:
                return pred
            if timeout is not None and (time.time() - start) > timeout:
                raise TimeoutError(f"[async] 예측 대기 시간이 초과되었습니다 (timeout={timeout}s). 현재 상태: {status}")
            await asyncio.sleep(interval)

    # ---------- 계정 점검 ----------
    def check_account(self) -> Dict[str, Any]:
        """
        토큰 유효성 확인. requests 가 없으면 replicate.run 으로 간단 체크 시도.
        """
        token = self.client.api_token  # type: ignore[attr-defined]
        if requests is None:
            # requests 가 없다면 아주 가벼운 no-op 호출 대신
            # 모델 호출 전 토큰 존재만 확인하는 수준으로 대체
            return {"ok": True, "note": "requests 미설치. 토큰 존재만 확인했습니다."}

        resp = requests.get(
            "https://api.replicate.com/v1/account",
            headers={"Authorization": f"Bearer {token}"},
            timeout=15,
        )
        if resp.ok:
            return {"ok": True, "account": resp.json()}
        return {"ok": False, "status": resp.status_code, "body": resp.text}


# ---------------- CLI ----------------
def _build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="sljeff/dots.ocr 클라이언트 CLI")
    sub = p.add_subparsers(dest="cmd", required=True)

    # run
    p_run = sub.add_parser("run", help="replicate.run 으로 간단 실행 (출력만 반환)")
    p_run.add_argument("--image", required=True, help="이미지 URL/로컬경로/dataURI")
    p_run.add_argument("--extra", nargs="*", default=[], help="추가 입력 key=value 형태 (예: --extra param1=foo param2=bar)")

    # create
    p_create = sub.add_parser("create", help="predictions.create 로 예측 객체 생성")
    p_create.add_argument("--image", required=True, help="이미지 URL/로컬경로/dataURI")
    p_create.add_argument("--webhook", help="웹훅 URL")
    p_create.add_argument("--webhook-events", nargs="*", help="웹훅 이벤트 목록 (start output logs completed)")
    p_create.add_argument("--wait", action="store_true", help="완료까지 대기")
    p_create.add_argument("--echo-logs", action="store_true", help="대기 중 logs 출력")
    p_create.add_argument("--timeout", type=float, default=300.0, help="대기 타임아웃(초)")
    p_create.add_argument("--interval", type=float, default=1.5, help="폴링 간격(초)")
    p_create.add_argument("--extra", nargs="*", default=[], help="추가 입력 key=value")

    # get
    p_get = sub.add_parser("get", help="prediction 조회")
    p_get.add_argument("id", help="prediction id")

    # cancel
    p_cancel = sub.add_parser("cancel", help="prediction 취소")
    p_cancel.add_argument("id", help="prediction id")

    # wait
    p_wait = sub.add_parser("wait", help="prediction 완료까지 대기")
    p_wait.add_argument("id", help="prediction id")
    p_wait.add_argument("--timeout", type=float, default=300.0)
    p_wait.add_argument("--interval", type=float, default=1.5)
    p_wait.add_argument("--echo-logs", action="store_true")

    # account
    sub.add_parser("account", help="토큰/계정 점검")

    return p


def _kv_list_to_dict(kvs: Iterable[str]) -> Dict[str, Any]:
    out: Dict[str, Any] = {}
    for item in kvs:
        if "=" not in item:
            raise ValueError(f"--extra 인자는 key=value 형태여야 합니다: {item}")
        k, v = item.split("=", 1)
        # JSON 값 지원 (숫자/불리언/객체 등)
        try:
            out[k] = json.loads(v)
        except json.JSONDecodeError:
            out[k] = v
    return out


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)

    client = DotsOCRClient()

    if args.cmd == "run":
        extra = _kv_list_to_dict(args.extra)
        result = client.run(args.image, **extra)
        print(json.dumps(result, ensure_ascii=False))
        return 0

    if args.cmd == "create":
        extra = _kv_list_to_dict(args.extra)
        pred = client.create_prediction(
            image=args.image,
            webhook=args.webhook,
            webhook_events_filter=args.webhook_events,
            **extra,
        )
        if args.wait:
            pred = client.wait(
                pred.id,  # type: ignore[attr-defined]
                interval=args.interval,
                timeout=args.timeout,
                echo_logs=args.echo_logs,
            )
        # Prediction 객체는 직렬화 가능 필드만 출력 시 가독성을 위해 dict 변환을 시도
        try:
            print(json.dumps(pred.__dict__, default=str, ensure_ascii=False))
        except Exception:
            print(pred)
        return 0

    if args.cmd == "get":
        pred = client.get_prediction(args.id)
        try:
            print(json.dumps(pred.__dict__, default=str, ensure_ascii=False))
        except Exception:
            print(pred)
        return 0

    if args.cmd == "cancel":
        pred = client.cancel_prediction(args.id)
        try:
            print(json.dumps(pred.__dict__, default=str, ensure_ascii=False))
        except Exception:
            print(pred)
        return 0

    if args.cmd == "wait":
        pred = client.wait(
            args.id,
            interval=args.interval,
            timeout=args.timeout,
            echo_logs=args.echo_logs,
        )
        try:
            print(json.dumps(pred.__dict__, default=str, ensure_ascii=False))
        except Exception:
            print(pred)
        return 0

    if args.cmd == "account":
        info = client.check_account()
        print(json.dumps(info, ensure_ascii=False))
        return 0

    parser.print_help()
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
