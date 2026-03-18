// Feature: android-aidemo-setup, Property 1: .gitignore 覆盖所有 Android 构建产物
package com.zkx.aidemo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * 属性测试：验证 .gitignore 覆盖所有 Android 标准构建产物路径。
 * 对于任意 Android 构建产物路径，git check-ignore 应返回退出码 0（即被忽略）。
 */
class GitignorePropertyTest : StringSpec({

    val buildArtifacts = listOf(
        "build/",
        "app/build/",
        ".gradle/",
        "local.properties",
        "app/release/app-release.apk",
        "app/release/app-release.aab",
        "captures/",
        ".cxx/",
        "app/src/main/something.ap_",
        "something.iml"
    )

    "对于任意 Android 构建产物路径，.gitignore 规则都应该匹配并忽略" {
        buildArtifacts.forEach { path ->
            val process = ProcessBuilder("git", "check-ignore", "-q", path)
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            exitCode shouldBe 0
        }
    }
})
