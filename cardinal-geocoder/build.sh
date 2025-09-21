#!/bin/sh

export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk}"

cargo build --release
cargo run --bin uniffi-bindgen generate --library target/release/libcardinal_geocoder.so --language kotlin --out-dir out

cargo ndk -t armeabi-v7a -t arm64-v8a -o ./jniLibs build --release

