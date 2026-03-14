# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

compose-rememberable は Kotlin コンパイラプラグインで、`@Rememberable` アノテーションを付けたクラスに対して `rememberSaveable` 用の `Saver<T, Bundle>` を自動生成する。Parcelable 不要でゼロボイラープレートの状態永続化を実現する。

## Build & Test Commands

```bash
# プロジェクト全体のビルド（sample-app含む）
./gradlew build

# コンパイラプラグインのテスト実行
./gradlew :rememberable-compiler:test

# ローカルMavenにパブリッシュ（sample-appでの動作確認用）
./gradlew publishToMavenLocal

# sample-appのビルド（プラグインをmavenLocalにパブリッシュ後）
./gradlew :sample-app:assembleDebug
```

## Architecture

4モジュール構成のKotlinコンパイラプラグインプロジェクト:

```
rememberable-annotations  → @Rememberable アノテーション定義（JVMライブラリ）
rememberable-compiler      → コンパイラプラグイン本体（FIR + IR）
rememberable-gradle-plugin → Gradleプラグイン（依存追加とコンパイラプラグイン設定を自動化）
sample-app                 → デモ用Androidアプリ
```

### コンパイラプラグインの処理フロー

1. **Gradle Plugin** (`KotlinCompilerPluginSupportPlugin`) がコンパイラプラグインを登録し `rememberable-annotations` 依存を自動追加
2. **FIR Phase** (`FirDeclarationGenerationExtension`) で `@Rememberable` クラスを検出し、companion object と `Saver` プロパティの宣言を合成
3. **IR Phase** (`IrElementTransformerVoid`) で `Saver` の実装を生成 — save/restoreラムダ内でBundleのput*/get*メソッドを型別に呼び分け

### ソースディレクトリ構成

標準のMavenレイアウトではなく、カスタムの `sourceSets` 設定を使用:
- compiler: `src/`, `resources/`, `test/`（`src/main/kotlin` ではない）
- annotations, gradle-plugin: 同様に `src/` 直下

### テスト

コンパイラプラグインのテストには `kctfork`（Kotlin Compiler Testing Fork）を使用。テスト内でAndroid SDK型（Bundle, Parcelable等）のモック実装を提供している。

## Key Technical Details

- **Kotlin**: 2.2.10（K2コンパイラ）
- **JVM Toolchain**: 21
- **Android**: compileSdk 36, minSdk 24
- **パブリッシュ**: GitHub Packages（`v*` タグプッシュで GitHub Actions が実行）
- **Configuration Cache**: 有効（publishタスクは `--no-configuration-cache` で実行）