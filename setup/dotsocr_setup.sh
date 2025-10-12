#!/bin/bash

# Initialize conda for bash
eval "$(conda shell.bash hook)"

# Create conda environment
conda create -n dots_ocr python=3.12 -y

# Activate conda environment
conda activate dots_ocr

# Navigate to the existing dots.ocr directory
cd third_party/dots.ocr

# Install pytorch (see https://pytorch.org/get-started/previous-versions/ for your cuda version)
pip install torch==2.7.0 torchvision==0.22.0 torchaudio==2.7.0 --index-url https://download.pytorch.org/whl/cu128

# Install dots.ocr in editable mode
pip install -e .
