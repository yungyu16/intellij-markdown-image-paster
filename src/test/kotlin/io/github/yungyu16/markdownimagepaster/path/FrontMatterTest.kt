package io.github.yungyu16.markdownimagepaster.path

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FrontMatterTest {

    @Test
    fun `parse returns null when no front matter present`() {
        val text = "# Hello\n\nThis is a regular markdown."
        assertNull(FrontMatterParser.parse(text))
    }

    @Test
    fun `parse extracts both typora-root-url and media_subpath`() {
        val text = """
            ---
            typora-root-url: /assets
            media_subpath: /assets/images
            title: Post
            ---
            # Body
        """.trimIndent()
        val fm = FrontMatterParser.parse(text)
        assertNotNull(fm)
        assertEquals("/assets", fm!!.typoraRootUrl)
        assertEquals("/assets/images", fm.mediaSubpath)
    }

    @Test
    fun `parse returns null for missing fields`() {
        val text = """
            ---
            title: Post
            author: alice
            ---
        """.trimIndent()
        val fm = FrontMatterParser.parse(text)
        assertNotNull(fm)
        assertNull(fm!!.typoraRootUrl)
        assertNull(fm.mediaSubpath)
    }

    @Test
    fun `parse handles front matter longer than 30 lines (regression I1)`() {
        val longField = "x".repeat(50)
        val text = buildString {
            appendLine("---")
            repeat(40) { i -> appendLine("field$i: value$i") }
            appendLine("typora-root-url: /$longField")
            appendLine("---")
        }
        val fm = FrontMatterParser.parse(text)
        assertNotNull(fm)
        assertEquals("/$longField", fm!!.typoraRootUrl)
    }

    @Test
    fun `parse handles non-ASCII content correctly`() {
        val text = """
            ---
            typora-root-url: /资源/图片
            title: 标题
            ---
            # 正文
        """.trimIndent()
        val fm = FrontMatterParser.parse(text)
        assertNotNull(fm)
        assertEquals("/资源/图片", fm!!.typoraRootUrl)
    }

    @Test
    fun `parse returns null when closing fence is missing`() {
        val text = """
            ---
            typora-root-url: /assets
            no closing fence
        """.trimIndent()
        assertNull(FrontMatterParser.parse(text))
    }

    @Test
    fun `parse handles leading UTF-8 BOM`() {
        val text = "﻿---\ntypora-root-url: /assets\n---\n# Body"
        val fm = FrontMatterParser.parse(text)
        assertNotNull(fm)
        assertEquals("/assets", fm!!.typoraRootUrl)
    }
}
