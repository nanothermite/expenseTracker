package xls

import java.io.File
import common.BaseObject
import entities.{Contact, Transactions, Uzer}
import org.apache.poi.ss.usermodel._
import scala.collection.JavaConversions._

/**
 * Created by hkatz on 3/17/16.
 */

object ProcessXLS {

  case class counter(var adds: Int, var dups: Int)
  object counter {
    def empty = counter(0, 0)
  }

  def parseRow(row: Row): BaseObject = {
    val rowSz = row.size
    Contact.empty
  }

  def readFile(xlsFile: File, uzer: Uzer, uploadType: String): counter = {
    implicit val uz = uzer
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

    val types:List[BaseObject] = {
      for {
        row <- sheet.rowIterator()
        cells = row.cellIterator.map(getCellString).toList
        nCells = cells.size
        obj = uploadType match {
          case "C" =>
            Contact.apply2(cells)
          case "E" =>
            Transactions.apply3(cells)
        }
      } yield obj
    }.toList.tail  // drop header line

    val counts = counter.empty

    types.foreach { baseobj =>
      uploadType match {
        case "C" =>
          val myContact = baseobj.asInstanceOf[Contact]
          val matched = Contact.findBiz(myContact.bizname)
          if (matched.isEmpty) {
            Contact.save(myContact)
            counts.adds = counts.adds + 1
          } else
            counts.dups = counts.dups + 1
        case "E" =>
          val myTrans = baseobj.asInstanceOf[Transactions]
          val matched = Transactions.findBiz(myTrans.trantype)
          if (matched.isEmpty) {
            Transactions.save(myTrans)
            counts.adds = counts.adds + 1
          } else
            counts.dups = counts.dups + 1
      }
    }
    counts
  }

}