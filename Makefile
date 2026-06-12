JAVA_HOME := $(shell jenv javahome)

export JAVA_HOME

.PHONY: build clean run dist publish help

## build   构建插件 ZIP
build: clean
	./gradlew buildPlugin

## clean   清理构建产物
clean:
	./gradlew clean

## run     在沙箱 IDEA 中运行（首次需下载 IDE，耗时较长）
run:
	./gradlew runIde

## publish 构建并发布插件到 JetBrains Marketplace
publish:
	./gradlew publishPlugin

## dist    显示构建产物路径
dist:
	@ls -lh build/distributions/*.zip 2>/dev/null || echo "尚未构建，请先执行 make build"

## help    显示帮助
help:
	@grep -E '^##' Makefile | sed 's/^## //'
