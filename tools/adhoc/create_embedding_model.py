import os
import shutil
import sys
import tempfile
from pathlib import Path

import onnx
from optimum.exporters.onnx import main_export
from optimum.onnxruntime import ORTQuantizer
from optimum.onnxruntime.configuration import AutoQuantizationConfig
from onnxruntime_extensions import gen_processing_models
from transformers import AutoTokenizer

REPO_ID = "intfloat/multilingual-e5-small"
QUANTIZED_MODEL_NAME = "model_quantized.onnx"
ORIGINAL_MODEL_NAME = "model.onnx"
TOKENIZER_MODEL_NAME = "tokenizer.onnx"
COPIED_EXPORT_ARTIFACTS = (
    "config.json",
    "ort_config.json",
)


def run_task():
    print(f"Start exporting model from: {REPO_ID}")

    output_dir = os.getenv("MODEL_DIR", "./model")
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    print(f"output_dir: {output_dir}")

    with tempfile.TemporaryDirectory(prefix="embedding-export-", dir=output_path.parent) as work_dir:
        work_path = Path(work_dir)
        print(f"work_dir: {work_dir}")

        try:
            main_export(
                REPO_ID,
                output=work_dir,
                trust_remote_code=True,
                task="feature-extraction",
                no_dynamic_axes=False,
            )
        except Exception as e:
            print(f"Error exporting model: {e}")
            raise e

        try:
            quantizer = ORTQuantizer.from_pretrained(work_dir)
            qconfig = AutoQuantizationConfig.avx2(
                is_static=False,
                per_channel=False
            )

            quantizer.quantize(
                save_dir=work_dir,
                quantization_config=qconfig
            )
        except Exception as e:
            print(f"Error quantizing model: {e}")
            raise e

        original_model_path = work_path / ORIGINAL_MODEL_NAME
        if original_model_path.exists():
            original_model_path.unlink()
            print(f"Removed {ORIGINAL_MODEL_NAME}. Using {QUANTIZED_MODEL_NAME} for inference.")

        quantized_model_path = work_path / QUANTIZED_MODEL_NAME
        shutil.copy2(quantized_model_path, output_path / QUANTIZED_MODEL_NAME)
        print(f"Copied quantized model to {output_path / QUANTIZED_MODEL_NAME}")

        for artifact_name in COPIED_EXPORT_ARTIFACTS:
            artifact_path = work_path / artifact_name
            if artifact_path.exists():
                shutil.copy2(artifact_path, output_path / artifact_name)
                print(f"Copied export artifact to {output_path / artifact_name}")

    print("Loading tokenizer...")
    try:
        tokenizer = AutoTokenizer.from_pretrained(
            REPO_ID, trust_remote_code=True)
    except Exception as e:
        print(f"Error loading tokenizer: {e}")
        raise e

    print("Generating tokenizer ONNX model from tokenizer...")
    try:
        preprocessing_models = gen_processing_models(tokenizer, pre_kwargs={})
        if not preprocessing_models:
            raise RuntimeError("No preprocessing ONNX model generated.")

        tokenizer_model = preprocessing_models[0]
        tokenizer_model_path = output_path / TOKENIZER_MODEL_NAME
        onnx.save_model(tokenizer_model, tokenizer_model_path)
        print(f"Saved tokenizer ONNX model to {tokenizer_model_path}")
    except Exception as e:
        print(f"Error generating tokenizer ONNX model: {e}")
        raise e

    print(f"Conversion completed.")
    print(f"Files saved in {output_dir}:")
    for f in output_path.iterdir():
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
