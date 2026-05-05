# Adhoc Function (Model Converter)

このツールは、Hugging Faceからモデルをダウンロードし、ONNX形式に変換してJavaバックエンドプロジェクトのリソースディレクトリに配置するためのスクリプトです。

## 概要

huggin face からモデルをダウンロードし、ONNX Runtime で利用可能な形式 (`model_quantized.onnx`, `tokenizer.onnx` 等) に変換します。Java 側は `tokenizer.onnx` と `model_quantized.onnx` の2段構成で利用します。
出力されたモデルファイルは `book-recommender-be` プロジェクトでローカル推論に使用されます。

## 前提条件

*   Docker

## 実行方法

OSやPythonのバージョン依存を避けるため、Dockerでの実行を前提としています。

### 1. イメージのビルド

```bash
docker build -t book-model-converter .
```

### 2. 実行

ホストマシンのディレクトリをマウントすることで、生成されたモデルをローカル（`./model`）に取り出すことができます。

```bash
docker run -it --rm \
  -v $(pwd)/model:/app/model \
  book-model-converter
```

## 生成されるファイル

*   `model_quantized.onnx`: ONNX形式のモデル本体
*   `tokenizer.onnx`: ONNX Runtime Extensions 用 tokenizer 前処理モデル
*   その他設定ファイル (`config.json` 等)

## S3へのアップロード

生成されたモデルファイルをAWS上のS3バケットにアップロードして、ECS等の実行環境で利用可能にします。

### アップロードコマンド

AWS CLIを使用して、ローカルのモデルディレクトリをS3バケットに同期します。

```bash
# `model_quantized.onnx` と `tokenizer.onnx` を主対象としてアップロードするします。
aws s3 cp ./model/model_quantized.onnx s3://book-model-bucket/model/model_quantized.onnx
aws s3 cp ./model/tokenizer.onnx s3://book-model-bucket/model/tokenizer.onnx
aws s3 cp ./model/config.json s3://book-model-bucket/model/config.json
```

### S3上のフォルダ構成

バックエンドアプリケーションの動作に合わせて、以下の構成でアップロードしてください。

```text
fitstyle-model-bucket/
└── model/
    ├── model_quantized.onnx         <-- ONNX形式のモデル本体
    ├── tokenizer.onnx               <-- tokenizer 前処理モデル
    └── config.json        <-- 構成ファイル
```

## 注意事項

*   `model_quantized.onnx` はファイルサイズが大きいため、Gitで管理する場合は **Git LFS** の利用を推奨します。
*   S3バケット名やプレフィックスを変更した場合は、インフラエンジニアまたは環境変数設定を確認してください。
*   Java API は `tokenizer.onnx` と `model_quantized.onnx` の2段構成で動作します。
