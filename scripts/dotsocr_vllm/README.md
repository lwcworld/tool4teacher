# dots.ocr vLLM Inference

This directory contains scripts for running vLLM inference with the dots.ocr model from [rednote-hilab/dots.ocr](https://huggingface.co/rednote-hilab/dots.ocr).

## Prerequisites

- Python 3.12
- CUDA-compatible GPU
- Conda (recommended)

## Installation

### 1. Create and activate conda environment

```bash
conda create -n dots_ocr python=3.12
conda activate dots_ocr
```

### 2. Install PyTorch

```bash
# Adjust CUDA version as needed (cu118, cu121, cu128, etc.)
pip install torch==2.7.0 torchvision==0.22.0 torchaudio==2.7.0 --index-url https://download.pytorch.org/whl/cu128
```

### 3. Install dots.ocr package

```bash
# From the repository root
pip install -e third_party/dots.ocr
```

### 4. Install inference dependencies

```bash
cd scripts/dotsocr
pip install -r requirements.txt
```

### 5. Download model weights

The model weights will be downloaded automatically when you first run the server setup script. Alternatively, you can download them manually:

```bash
cd ../../third_party/dots.ocr
python3 tools/download_model.py
```

## Usage

### Starting the vLLM Server

Use the provided setup script to launch the vLLM server:

```bash
./setup_server.sh
```

**Customize server settings with environment variables:**

```bash
# Use different GPU
GPU_ID=1 ./setup_server.sh

# Change port
PORT=8080 ./setup_server.sh

# Adjust GPU memory utilization
GPU_MEMORY_UTIL=0.8 ./setup_server.sh

# Multi-GPU setup
TENSOR_PARALLEL_SIZE=2 GPU_ID=0,1 ./setup_server.sh
```

**All available environment variables:**

- `GPU_ID`: GPU device ID(s) to use (default: 0)
- `PORT`: Server port (default: 8000)
- `TENSOR_PARALLEL_SIZE`: Number of GPUs for tensor parallelism (default: 1)
- `GPU_MEMORY_UTIL`: GPU memory utilization ratio (default: 0.95)
- `MODEL_NAME`: Model name for vLLM (default: model)

### Running Inference

Once the server is running, use the inference script in a separate terminal:

```bash
# Basic usage
python inference.py --image path/to/image.jpg

# Layout detection only
python inference.py --image path/to/image.jpg --prompt_mode prompt_layout_only_en

# OCR text extraction
python inference.py --image path/to/image.jpg --prompt_mode prompt_ocr

# Save output to file
python inference.py --image path/to/image.jpg --output results.json

# Connect to remote server
python inference.py --image path/to/image.jpg --ip 192.168.1.100 --port 8000
```

**Available prompt modes:**

- `prompt_layout_all_en` (default): Parse all layout info including bbox, category, and text in JSON format
- `prompt_layout_only_en`: Layout detection only (no text extraction)
- `prompt_ocr`: Extract all text content from the image
- `prompt_grounding_ocr`: Extract text from a given bounding box

**Full command options:**

```bash
python inference.py \
  --image path/to/image.jpg \
  --prompt_mode prompt_layout_all_en \
  --ip localhost \
  --port 8000 \
  --model_name model \
  --temperature 0.1 \
  --top_p 0.9 \
  --max_tokens 32768 \
  --output results.json
```

## Examples

### Example 1: Full Layout Analysis

```bash
python inference.py --image document.pdf.jpg --output layout.json
```

This will extract all layout elements with their bounding boxes, categories, and text content.

### Example 2: Quick OCR

```bash
python inference.py --image screenshot.png --prompt_mode prompt_ocr
```

This will extract all text from the image without layout information.

### Example 3: Batch Processing

```bash
for img in images/*.jpg; do
  python inference.py --image "$img" --output "results/$(basename "$img" .jpg).json"
done
```

## Output Format

The output format depends on the prompt mode used:

### prompt_layout_all_en

Returns a JSON array with layout elements:

```json
[
  {
    "bbox": [x1, y1, x2, y2],
    "category": "Text",
    "text": "Content in markdown format"
  },
  {
    "bbox": [x1, y1, x2, y2],
    "category": "Table",
    "text": "<table>...</table>"
  }
]
```

**Layout categories:** Caption, Footnote, Formula, List-item, Page-footer, Page-header, Picture, Section-header, Table, Text, Title

**Text formatting:**
- Formula: LaTeX format
- Table: HTML format
- Others: Markdown format

## Troubleshooting

### Server won't start

- Check if the port is already in use: `lsof -i :8000`
- Verify CUDA is available: `python -c "import torch; print(torch.cuda.is_available())"`
- Check GPU memory: `nvidia-smi`

### Out of memory errors

Reduce GPU memory utilization:

```bash
GPU_MEMORY_UTIL=0.7 ./setup_server.sh
```

### Connection refused errors

- Ensure the vLLM server is running
- Check firewall settings if connecting remotely
- Verify the IP and port match the server configuration

## Notes

- The vLLM server must be running before executing inference
- For optimal performance, use vLLM version 0.9.1 as recommended by the model authors
- The model weights are stored in `third_party/dots.ocr/weights/DotsOCR`
- The `third_party/dots.ocr` directory should not be modified

## References

- Model: [rednote-hilab/dots.ocr](https://huggingface.co/rednote-hilab/dots.ocr)
- GitHub: [dots.ocr repository](https://github.com/rednote-hilab/dots.ocr)
