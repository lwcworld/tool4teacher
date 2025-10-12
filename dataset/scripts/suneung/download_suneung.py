#!/usr/bin/env python3
"""
수능 시험지 자동 다운로드 스크립트

Usage:
    python scripts/download_suneung.py --year 2024 --output downloads/
    python scripts/download_suneung.py --year 2024 --subject 국어 --max-pages 5
"""

import argparse
import logging
import os
import re
import time
from pathlib import Path
from typing import List, Dict, Optional
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.common.exceptions import TimeoutException, NoSuchElementException

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class SuneungDownloader:
    """수능 시험지 다운로드 클래스"""

    BASE_URL = "https://www.suneung.re.kr"
    LIST_URL = "https://www.suneung.re.kr/boardCnts/list.do"

    def __init__(self, output_dir: str = "downloads", headless: bool = True):
        """
        Args:
            output_dir: 다운로드 디렉토리
            headless: 브라우저를 백그라운드에서 실행할지 여부
        """
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.headless = headless
        self.driver = None
        self.session = requests.Session()

    def _init_driver(self):
        """Chrome 드라이버 초기화"""
        if self.driver is not None:
            return

        chrome_options = Options()
        if self.headless:
            chrome_options.add_argument('--headless')
        chrome_options.add_argument('--no-sandbox')
        chrome_options.add_argument('--disable-dev-shm-usage')
        chrome_options.add_argument('--disable-gpu')
        chrome_options.add_argument('--window-size=1920,1080')

        # 다운로드 설정
        prefs = {
            "download.default_directory": str(self.output_dir.absolute()),
            "download.prompt_for_download": False,
            "download.directory_upgrade": True,
            "safebrowsing.enabled": False,
            "plugins.always_open_pdf_externally": True
        }
        chrome_options.add_experimental_option("prefs", prefs)

        try:
            self.driver = webdriver.Chrome(options=chrome_options)
            logger.info("Chrome 드라이버 초기화 완료")
        except Exception as e:
            logger.error(f"Chrome 드라이버 초기화 실패: {e}")
            raise

    def close(self):
        """리소스 정리"""
        if self.driver:
            self.driver.quit()
            self.driver = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()

    def get_page_content(self, page: int = 1) -> List[Dict]:
        """
        페이지의 시험지 목록 가져오기

        Args:
            page: 페이지 번호 (1부터 시작)

        Returns:
            시험지 정보 리스트
        """
        self._init_driver()

        params = {
            'boardID': '1500234',
            'm': '0403',
            's': 'suneung',
            'searchStr': '',
            'type': 'default',
            'page': str(page)
        }

        url = self.LIST_URL + '?' + '&'.join([f"{k}={v}" for k, v in params.items()])
        logger.info(f"페이지 {page} 로딩 중: {url}")

        try:
            self.driver.get(url)
            time.sleep(2)  # 페이지 로딩 대기

            # 페이지 소스를 BeautifulSoup으로 파싱
            soup = BeautifulSoup(self.driver.page_source, 'html.parser')

            papers = []

            # 테이블 찾기
            tables = soup.find_all('table')
            if not tables:
                logger.warning(f"페이지 {page}에서 테이블을 찾을 수 없습니다.")
                return []

            # 첫 번째 테이블 사용 (데스크톱 버전)
            table = tables[0]
            rows = table.find_all('tr')

            for row in rows:
                try:
                    cells = row.find_all('td')
                    if len(cells) < 7:  # 번호, 학년도, 영역, 제목, 등록일, 조회, 파일
                        continue

                    # 각 셀에서 정보 추출
                    # 셀 구조: [번호, 학년도, 영역, 제목, 등록일, 조회, 파일]
                    year_cell = cells[1].get_text(strip=True)
                    subject_cell = cells[2].get_text(strip=True)
                    title_cell = cells[3].get_text(strip=True)
                    file_cell = cells[6]

                    # 학년도와 영역으로 전체 제목 구성
                    full_title = f"{year_cell}학년도 {subject_cell} {title_cell}"

                    # 다운로드 링크 찾기 (파일 셀에서)
                    download_links = []
                    links = file_cell.find_all('a', href=True)
                    for link in links:
                        onclick = link.get('onclick', '')

                        # fileSeq 추출 (fn_fileDown 함수의 인자)
                        if 'fn_fileDown' in onclick:
                            # fn_fileDown('파일ID') 형태에서 ID 추출
                            match = re.search(r"fn_fileDown\(['\"]([^'\"]+)['\"]\)", onclick)
                            if match:
                                file_seq = match.group(1)
                                download_links.append({
                                    'file_seq': file_seq,
                                    'element': link
                                })

                    if download_links:
                        paper_info = {
                            'title': full_title,
                            'year': year_cell,
                            'subject': subject_cell,
                            'download_links': download_links,
                            'page': page
                        }
                        papers.append(paper_info)
                        logger.debug(f"발견: {full_title} ({len(download_links)}개 파일)")

                except Exception as e:
                    logger.debug(f"행 파싱 중 오류: {e}")
                    continue

            logger.info(f"페이지 {page}에서 {len(papers)}개 시험지 발견")
            return papers

        except Exception as e:
            logger.error(f"페이지 {page} 로딩 실패: {e}")
            return []

    def download_file(self, file_seq: str, title: str) -> bool:
        """
        파일 다운로드

        Args:
            file_seq: 파일 시퀀스 번호
            title: 시험지 제목

        Returns:
            다운로드 성공 여부
        """
        try:
            # 다운로드 URL 직접 구성
            download_url = f"{self.BASE_URL}/boardCnts/fileDown.do?fileSeq={file_seq}"
            logger.info(f"다운로드 시작: {title} (fileSeq: {file_seq})")

            # 현재 파일 목록 저장
            before_files = set(self.output_dir.glob("*"))

            # URL로 직접 이동하여 다운로드
            self.driver.get(download_url)

            # 다운로드 완료 대기 (최대 30초)
            timeout = 30
            start_time = time.time()

            # 초기 대기 (다운로드 시작 대기)
            time.sleep(2)

            while time.time() - start_time < timeout:
                time.sleep(0.5)

                # 현재 파일 목록 확인
                after_files = set(self.output_dir.glob("*"))
                new_files = after_files - before_files

                # 다운로드 진행 중인 파일 확인
                downloading = list(self.output_dir.glob("*.crdownload")) + \
                             list(self.output_dir.glob("*.tmp"))

                # 새 파일이 생성되었고 진행 중인 다운로드가 없으면 완료
                if new_files and not downloading:
                    logger.info(f"다운로드 완료: {title}")
                    return True

            logger.warning(f"다운로드 타임아웃: {title}")
            return False

        except Exception as e:
            logger.error(f"다운로드 실패: {title} - {e}")
            return False

    def download_papers(
        self,
        year_filter: Optional[str] = None,
        subject_filter: Optional[str] = None,
        max_pages: int = 10
    ):
        """
        시험지 다운로드

        Args:
            year_filter: 연도 필터 (예: "2024")
            subject_filter: 과목 필터 (예: "국어", "수학")
            max_pages: 최대 페이지 수
        """
        self._init_driver()

        total_downloaded = 0
        total_failed = 0

        try:
            for page in range(1, max_pages + 1):
                logger.info(f"\n{'='*60}")
                logger.info(f"페이지 {page}/{max_pages} 처리 중")
                logger.info(f"{'='*60}")

                papers = self.get_page_content(page)

                if not papers:
                    logger.warning(f"페이지 {page}에 시험지가 없습니다. 종료합니다.")
                    break

                for paper in papers:
                    title = paper['title']
                    year = paper.get('year', '')
                    subject = paper.get('subject', '')

                    # 필터 적용
                    if year_filter and year_filter != year:
                        logger.debug(f"연도 필터로 제외: {title}")
                        continue

                    if subject_filter and subject_filter not in subject:
                        logger.debug(f"과목 필터로 제외: {title}")
                        continue

                    # 다운로드 링크 처리
                    for link_info in paper['download_links']:
                        success = self.download_file(link_info['file_seq'], title)
                        if success:
                            total_downloaded += 1
                        else:
                            total_failed += 1

                        # 다운로드 사이 대기
                        time.sleep(1)

                # 페이지 사이 대기
                time.sleep(3)

        except KeyboardInterrupt:
            logger.info("\n사용자에 의해 중단되었습니다.")
        except Exception as e:
            logger.error(f"다운로드 중 오류 발생: {e}")
        finally:
            logger.info(f"\n{'='*60}")
            logger.info(f"다운로드 완료: {total_downloaded}개 성공, {total_failed}개 실패")
            logger.info(f"저장 위치: {self.output_dir.absolute()}")
            logger.info(f"{'='*60}")


def main():
    parser = argparse.ArgumentParser(
        description='수능 시험지 자동 다운로드 스크립트',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
예제:
  # 2024년 시험지 모두 다운로드
  python scripts/download_suneung.py --year 2024

  # 2024년 국어 시험지만 다운로드
  python scripts/download_suneung.py --year 2024 --subject 국어

  # 최근 5페이지만 다운로드
  python scripts/download_suneung.py --max-pages 5

  # 브라우저 화면 표시하며 다운로드
  python scripts/download_suneung.py --no-headless
        """
    )

    parser.add_argument(
        '--year',
        type=str,
        help='다운로드할 연도 (예: 2024)'
    )

    parser.add_argument(
        '--subject',
        type=str,
        help='다운로드할 과목 (예: 국어, 수학, 영어)'
    )

    parser.add_argument(
        '--output',
        type=str,
        default='downloads/suneung',
        help='다운로드 디렉토리 (기본값: downloads/suneung)'
    )

    parser.add_argument(
        '--max-pages',
        type=int,
        default=10,
        help='최대 페이지 수 (기본값: 10)'
    )

    parser.add_argument(
        '--no-headless',
        action='store_true',
        help='브라우저 화면 표시'
    )

    parser.add_argument(
        '--verbose',
        action='store_true',
        help='상세 로그 출력'
    )

    args = parser.parse_args()

    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    logger.info("수능 시험지 다운로드 시작")
    logger.info(f"설정: 연도={args.year or '전체'}, 과목={args.subject or '전체'}, "
               f"최대 페이지={args.max_pages}")

    with SuneungDownloader(
        output_dir=args.output,
        headless=not args.no_headless
    ) as downloader:
        downloader.download_papers(
            year_filter=args.year,
            subject_filter=args.subject,
            max_pages=args.max_pages
        )


if __name__ == '__main__':
    main()
