#!/bin/bash
# Setup script for dots.ocr vLLM server

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== dots.ocr vLLM Server Setup ===${NC}\n"

# Default values
GPU_ID=${GPU_ID:-0}
TENSOR_PARALLEL_SIZE=${TENSOR_PARALLEL_SIZE:-1}
GPU_MEMORY_UTIL=${GPU_MEMORY_UTIL:-0.95}
PORT=${PORT:-8000}
MODEL_NAME=${MODEL_NAME:-model}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$SCRIPT_DIR/../.."
DOTS_OCR_DIR="$REPO_ROOT/third_party/dots.ocr"

# Check if dots.ocr directory exists
if [ ! -d "$DOTS_OCR_DIR" ]; then
    echo -e "${RED}Error: dots.ocr not found at $DOTS_OCR_DIR${NC}"
    exit 1
fi

# Check if model weights exist
WEIGHTS_DIR="$DOTS_OCR_DIR/weights/DotsOCR"
if [ ! -d "$WEIGHTS_DIR" ]; then
    echo -e "${YELLOW}Model weights not found. Downloading...${NC}"
    cd "$DOTS_OCR_DIR"
    python3 tools/download_model.py
    cd "$SCRIPT_DIR"
fi

# Set model path
export hf_model_path="$WEIGHTS_DIR"

# Check if vllm is installed
if ! command -v vllm &> /dev/null; then
    echo -e "${RED}Error: vllm is not installed${NC}"
    echo -e "${YELLOW}Please install vllm first:${NC}"
    echo "  pip install vllm"
    exit 1
fi

# Register model with vLLM
echo -e "${GREEN}Registering DotsOCR model with vLLM...${NC}"
export PYTHONPATH=$(dirname "$hf_model_path"):$PYTHONPATH

# Check if vllm entry point is already patched
VLLM_PATH=$(which vllm)
if ! grep -q "from DotsOCR import modeling_dots_ocr_vllm" "$VLLM_PATH"; then
    echo -e "${YELLOW}Patching vllm entry point...${NC}"
    sed -i '/^from vllm\.entrypoints\.cli\.main import main$/a\
from DotsOCR import modeling_dots_ocr_vllm' "$VLLM_PATH"
    echo -e "${GREEN}vllm entry point patched successfully${NC}"
else
    echo -e "${GREEN}vllm entry point already patched${NC}"
fi

echo -e "${GREEN}Configuration:${NC}"
echo "  GPU ID: $GPU_ID"
echo "  Tensor Parallel Size: $TENSOR_PARALLEL_SIZE"
echo "  GPU Memory Utilization: $GPU_MEMORY_UTIL"
echo "  Port: $PORT"
echo "  Model Name: $MODEL_NAME"
echo "  Model Path: $hf_model_path"
echo ""

# Launch vLLM server
echo -e "${GREEN}Launching vLLM server...${NC}"
CUDA_VISIBLE_DEVICES=$GPU_ID vllm serve "$hf_model_path" \
  --tensor-parallel-size "$TENSOR_PARALLEL_SIZE" \
  --gpu-memory-utilization "$GPU_MEMORY_UTIL" \
  --chat-template-content-format string \
  --served-model-name "$MODEL_NAME" \
  --port "$PORT" \
  --trust-remote-code
