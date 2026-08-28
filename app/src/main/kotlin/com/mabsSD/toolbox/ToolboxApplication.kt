package com.mabsSD.toolbox

import android.app.Application
import android.content.Context
import com.mabsSD.toolbox.tools.CopyFileTool
import com.mabsSD.toolbox.tools.ToolRegistry
import com.mabsSD.toolbox.utils.WorkingFileManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Minimal service locator for app-wide singletons. Kept deliberately small;
 * dependency injection (Hilt) can replace it later without touching screens.
 */
class AppContainer(context: Context) {
    val workingFileManager: WorkingFileManager = WorkingFileManager(context)
    val copyFileTool: CopyFileTool = CopyFileTool(context, workingFileManager)
}

class ToolboxApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
        container.workingFileManager.init()
        ToolRegistry.register(container.copyFileTool)
    }
}
