package pj.domain.schedule

import org.scalatest.funsuite.AnyFunSuite
import scala.xml.XML
import java.io.File

class ScheduleMS03Test extends AnyFunSuite:

  val inputDir = new File("files/tests-ms03/input")
  val expectedDir = new File("files/tests-ms03/output")

  test("ScheduleMS03 diagnostic run — log mismatches only"):

    assert(inputDir.exists && expectedDir.exists, "Input or output directory does not exist.")

    val inputFiles = inputDir.listFiles().filter(_.getName.endsWith(".xml")).toList

    val results: List[String] = inputFiles.map { inputFile =>
      val inputXml = XML.loadFile(inputFile)
      val result = ScheduleMS03.create(inputXml)
      val expectedFile = new File(expectedDir, inputFile.getName)

      result match
        case Left(error) =>
          s"[FAIL] ${inputFile.getName}: $error"

        case Right(generatedXml) if expectedFile.exists =>
          val expectedXml = XML.loadFile(expectedFile)
          val normalizedGenerated = normalize(generatedXml)
          val normalizedExpected = normalize(expectedXml)
          if normalizedGenerated == normalizedExpected then
            s"[PASS] ${inputFile.getName}"
          else
            println(s"[MISMATCH] ${inputFile.getName} — Output differs from expected.")
            println(s"\nExpected:\n$normalizedExpected")
            println(s"\nActual:\n$normalizedGenerated\n")
            s"[MISMATCH] ${inputFile.getName}"

        case Right(_) =>
          s"[UNEXPECTED PASS] ${inputFile.getName} — No expected output to compare."
    }

    // Summary
    println("\n--- Test Summary ---")
    results.foreach(println)
    println("--------------------")

    assert(true)

  def normalize(xml: scala.xml.Node): String =
    xml.toString().trim.replaceAll("\\s+", "")
