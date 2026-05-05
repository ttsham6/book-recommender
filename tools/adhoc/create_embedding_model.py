import os
import sys
from pathlib import Path
from optimum.exporters.onnx import main_export
from optimum.onnxruntime import ORTQuantizer
from optimum.onnxruntime.configuration import AutoQuantizationConfig
from transformers import AutoTokenizer

REPO_ID = "intfloat/multilingual-e5-small"

def run_task():
    print(f"Start exporting model from: {REPO_ID}")
    
    output_dir = os.getenv("MODEL_DIR", "./model")
    print(f"output_dir: {output_dir}")
    
    try:
        main_export(
            REPO_ID,
            output=output_dir,
            trust_remote_code=True,
            task="feature-extraction",
            no_dynamic_axes=False,
        )
    except Exception as e:
        print(f"Error exporting model: {e}")
        raise e

    try:
        quantizer = ORTQuantizer.from_pretrained("./model")
        qconfig = AutoQuantizationConfig.avx2(
            is_static=False,
            per_channel=False
        )

        quantizer.quantize(
            save_dir="./model",
            quantization_config=qconfig
        )
    except Exception as e:
        print(f"Error quantizing model: {e}")
        raise e

    print("Loading and saving tokenizer...")
    try:
        tokenizer = AutoTokenizer.from_pretrained(REPO_ID, trust_remote_code=True)
        tokenizer.save_pretrained(output_dir)
    except Exception as e:
        print(f"Error loading/saving tokenizer: {e}")
        raise e

    print(f"Conversion completed.")
    print(f"Files saved in {output_dir}:")
    for f in Path(output_dir).iterdir():
        print(f"  - {f.name}")

if __name__ == "__main__":
    try:
        run_task()
        print("[SUCCESS] Task completed successfully.")
        sys.exit(0)
    except Exception as e:
        print(f"[ERROR] Task failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)