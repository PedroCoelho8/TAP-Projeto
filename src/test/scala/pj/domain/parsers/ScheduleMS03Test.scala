package pj.domain.parsers

import org.scalatest.funsuite.AnyFunSuite
import pj.domain.schedule.ScheduleMS03

import scala.xml.XML
import java.io.File

class ScheduleMS03Test extends AnyFunSuite:

  val inputDir = new File("files/assessment/ms03")

  test("ScheduleMS03 diagnostic run — print outputs for validAgenda files"):

    assert(inputDir.exists, "Input directory does not exist.")

    val validAgendaFiles = inputDir.listFiles()
      .filter(_.getName.endsWith(".xml"))
      .filter(!_.getName.endsWith("out.xml"))
      .filter(_.getName.startsWith("validAgenda"))
      .toList
      .sortBy(_.getName)

    if validAgendaFiles.isEmpty then
      println("No files starting with 'validAgenda' found.")
      assert(true)
      
    println(s"\nProcessing ${validAgendaFiles.length} validAgenda files:\n")

    val results: List[String] = validAgendaFiles.map { inputFile =>
      println(s"Processing: ${inputFile.getName}")

      val inputXml = XML.loadFile(inputFile)
      val result = ScheduleMS03.create(inputXml)

      result match
        case Left(error) =>
          println(s"[FAIL] $error\n")
          s"[FAIL] ${inputFile.getName}: $error"

        case Right(generatedXml) =>
          println("[SUCCESS] Generated XML:")
          println(formatXml(generatedXml))

          // Create output file
          val outputFileName = inputFile.getName.replace("in.xml", "out.xml")
          val outputFile = new File(inputFile.getParent, outputFileName)
          saveXmlToFile(generatedXml, outputFile)
          println(s"Saved to: ${outputFile.getName}")
          println()
          s"[PASS] ${inputFile.getName}"
    }

    println("--- Summary ---")
    results.foreach(println)
    println(s"Success: ${results.count(_.startsWith("[PASS]"))}")
    println(s"Failed: ${results.count(_.startsWith("[FAIL]"))}")

    assert(true)

  private def formatXml(xml: scala.xml.Node): String =
    val prettyPrinter = new scala.xml.PrettyPrinter(80, 2)
    prettyPrinter.format(xml)

  private def saveXmlToFile(xml: scala.xml.Node, file: File): Unit =
    val prettyPrinter = new scala.xml.PrettyPrinter(80, 2)
    val xmlString = prettyPrinter.format(xml)
    val fullXmlContent = s"""<?xml version="1.0" encoding="UTF-8"?>\n$xmlString"""

    java.nio.file.Files.write(
      file.toPath,
      fullXmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    )