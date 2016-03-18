package xls

import java.io.File

import common.BaseObject
import entities.Contact
import org.apache.poi.ss.usermodel._
import scala.collection.JavaConversions._

/**
 * Created by hkatz on 3/17/16.
 */

object ProcessXLS {

  def parseRow(row: Row): BaseObject = {
    val rowSz = row.size
    Contact.apply(None, None, None, None, None, None)
  }


  def readFile(xlsFile: File, uploadType: String) = {
    val wb = WorkbookFactory.create(xlsFile)

    def getCellString(cell: Cell) = {
      cell.getCellType() match {
        case Cell.CELL_TYPE_NUMERIC =>
          (new DataFormatter()).formatCellValue(cell)
        case Cell.CELL_TYPE_STRING =>
          cell.getStringCellValue()
        case Cell.CELL_TYPE_FORMULA =>
          val evaluator = wb.getCreationHelper().createFormulaEvaluator()
          (new DataFormatter()).formatCellValue(cell, evaluator)
        case _ => " "
      }
    }

    val sheet = wb.getSheetAt(0)

    val rows = sheet.rowIterator.size


    /*val types:List[BaseObject] = for {
      row <- sheet.rowIterator()

    } yield */
    val text = sheet.rowIterator.map { row: Row =>
      row.cellIterator.map(getCellString).mkString("|")
    }.mkString("\n")
  }

}