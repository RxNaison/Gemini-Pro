package com.rx.geminipro

import com.rx.geminipro.components.generateHtmlWithMermaid
import com.rx.geminipro.components.getMermaidDiagramPrefixes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagramCodesTest {

    @Test
    fun getMermaidDiagramPrefixes_containsCoreDiagramTypes() {
        val prefixes = getMermaidDiagramPrefixes()

        assertTrue(prefixes.isNotEmpty())
        assertTrue("Prefixes should contain 'graph'", prefixes.contains("graph"))
        assertTrue("Prefixes should contain 'flowchart'", prefixes.contains("flowchart"))
        assertTrue("Prefixes should contain 'sequenceDiagram'", prefixes.contains("sequenceDiagram"))
        assertTrue("Prefixes should contain 'classDiagram'", prefixes.contains("classDiagram"))
        assertTrue("Prefixes should contain 'stateDiagram'", prefixes.contains("stateDiagram"))
        assertTrue("Prefixes should contain 'erDiagram'", prefixes.contains("erDiagram"))
        assertTrue("Prefixes should contain 'pie'", prefixes.contains("pie"))
        assertTrue("Prefixes should contain 'gantt'", prefixes.contains("gantt"))
        assertTrue("Prefixes should contain 'mindmap'", prefixes.contains("mindmap"))
    }

    @Test
    fun generateHtmlWithMermaid_embedsCodeAndScript() {
        val mermaidCode = "graph TD\n    A[Start] --> B[End]"
        val html = generateHtmlWithMermaid(mermaidCode)

        assertTrue(html.contains("<pre class=\"mermaid\">$mermaidCode</pre>"))
        assertTrue(html.contains("mermaid.initialize"))
        assertTrue(html.contains("import mermaid"))
        assertFalse(html.isBlank())
    }
}
