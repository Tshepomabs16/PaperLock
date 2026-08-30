package com.mabsSD.toolbox

import android.app.Application
import android.content.Context
import com.mabsSD.toolbox.pdf.PdfBoxEngine
import com.mabsSD.toolbox.pdf.PdfEngine
import com.mabsSD.toolbox.tools.CopyFileTool
import com.mabsSD.toolbox.tools.MergeTool
import com.mabsSD.toolbox.tools.SplitTool
import com.mabsSD.toolbox.tools.ToolRegistry
import com.mabsSD.toolbox.utils.WorkingFileManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Minimal service locator for app-wide singletons. Kept deliberately small;
 * dependency injection (Hilt) can replace it later without touching screens.
 */
class AppContainer(context: Context) {
    val workingFileManager: WorkingFileManager = WorkingFileManager(context)

    /** The one PdfEngine instance; every PDF operation in the app uses it. */
    val pdfEngine: PdfEngine = PdfBoxEngine()

    val copyFileTool: CopyFileTool = CopyFileTool(context, workingFileManager)
    val splitTool: SplitTool = SplitTool(context, workingFileManager, pdfEngine)
    val mergeTool: MergeTool = MergeTool(context, workingFileManager, pdfEngine)
}

class ToolboxApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
        container.workingFileManager.init()

        // Registering a tool is what makes its Home tile tappable, so this list
        // is the single source of truth for what the app can actually do.
        ToolRegistry.register(container.copyFileTool)
        ToolRegistry.register(container.splitTool)
        ToolRegistry.register(container.mergeTool)
    }
}
