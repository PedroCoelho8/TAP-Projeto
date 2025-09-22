package pj

import pj.domain.parsers.ProductionParser
import pj.io.FileIO
import pj.xml.XMLWriter
/*
@main def run(): Unit =
  //ProductionParser.parseProduction("input.xml") match {
    case Right(prod) =>
      println("Production parsed successfully.")
    // Do something with `prod`, or write it somewhere

    case Left(err) =>
      val xmlErr = XMLWriter.toXmlError(err)
      FileIO.save("error.xml", xmlErr)
      println("Error details saved to error.xml")
  }*/