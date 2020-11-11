@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.ConsoleReport
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

/**
 * Produces human-readable advice files and console report on how to modify a project's
 * dependencies in order to have a healthy build.
 */
@CacheableTask
abstract class AdvicePrinterTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Displays advice on screen"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val adviceConsoleReport: RegularFileProperty

  @get:Input
  abstract val dependencyRenamingMap: MapProperty<String, String>

  @get:OutputFile
  abstract val adviceConsoleReportTxt: RegularFileProperty

  @TaskAction
  fun action() {
    // Output
    val adviceConsoleReportTxtFile = adviceConsoleReportTxt.getAndDelete()

    // Inputs
    val consoleReport = adviceConsoleReport.fromJson<ConsoleReport>()

    val consoleReportText = StringBuilder()

    if (consoleReport.isEmpty()) {
      consoleReportText.append("Looking good! No changes needed")
    } else {
      val advicePrinter = AdvicePrinter(consoleReport, dependencyRenamingMap.orNull)
      var didGiveAdvice = false

      advicePrinter.getRemoveAdvice()?.let {
        consoleReportText.appendReproducibleNewLine("Unused dependencies which should be removed:\n$it\n")
        didGiveAdvice = true
      }

      advicePrinter.getAddAdvice()?.let {
        consoleReportText.appendReproducibleNewLine("Transitively used dependencies that should be declared directly as indicated:\n$it\n")
        didGiveAdvice = true
      }

      advicePrinter.getChangeAdvice()?.let {
        consoleReportText.appendReproducibleNewLine("Existing dependencies which should be modified to be as indicated:\n$it\n")
        didGiveAdvice = true
      }

      advicePrinter.getCompileOnlyAdvice()?.let {
        consoleReportText.appendReproducibleNewLine("Dependencies which could be compile-only:\n$it\n")
        didGiveAdvice = true
      }

      advicePrinter.getRemoveProcAdvice()?.let {
        consoleReportText.appendReproducibleNewLine("Unused annotation processors that should be removed:\n$it\n")
        didGiveAdvice = true
      }

      if (didGiveAdvice) {
        consoleReportText.append("See console report at ${adviceConsoleReportTxtFile.path}")
      } else {
        consoleReportText.append("Looking good! No changes needed")
      }

      val reportText = consoleReportText.toString()
      logger.debug(reportText)
      adviceConsoleReportTxtFile.writeText(reportText)
    }
  }
}
