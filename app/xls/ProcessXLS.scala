package xls

import java.io.File
import common.BaseObject
import entities.{TransactionsXLS, Contact, Transactions, Uzer}
import org.apache.poi.ss.usermodel._
import scala.collection.JavaConversions._

/**
 * Created by hkatz on 3/17/16.
 */

object ProcessXLS {

  case class counter(adds: Int, dups: Int)

  def parseRow(row: Row): BaseObject = {
    val rowSz = row.size
    Contact.empty
  }

  def readFile(xlsFile: File, uzer: Uzer, uploadType: String): counter = {
    implicit val uz = uzer
    val wb = WorkbookFactory.create(xlsFile)

    def getCellString(cell: Cell) = {
      cell.getCellType match {
        case Cell.CELL_TYPE_NUMERIC =>
          new DataFormatter().formatCellValue(cell)
        case Cell.CELL_TYPE_STRING =>
          cell.getStringCellValue
        case Cell.CELL_TYPE_FORMULA =>
          val evaluator = wb.getCreationHelper.createFormulaEvaluator()
          new DataFormatter().formatCellValue(cell, evaluator)
        case _ => " "
      }
    }

    val sheet = wb.getSheetAt(0)
    val cellRange = Range(0, uploadType match {
      case "C" => 6
      case "E" => 7
    }).toList.iterator

    val types:List[BaseObject] = {
      for {
        row <- sheet.rowIterator().toList.tail
        cells = cellRange.map { cellNdx =>
          val cellVal = row.getCell(cellNdx)
          if (cellVal != null) getCellString(cellVal) else " "
        }.toList
        nCells = cells.size
        obj = uploadType match {
          case "C" =>
            Contact.apply2(cells)
          case "E" =>
            TransactionsXLS.apply(cells)
        }
      } yield obj
    }

    val stats: List[Boolean] =
      types.map { baseobj =>
        uploadType match {
          case "C" =>
            val myContact = baseobj.asInstanceOf[Contact]
            val matched = Contact.findBiz(myContact.bizname)
            if (matched.isEmpty) {
              Contact.save(myContact)
              true
            } else
              false
          case "E" =>
            val myTrans = baseobj.asInstanceOf[TransactionsXLS]
            val (matched, contact) = TransactionsXLS.findBiz(myTrans)
            if (matched.isEmpty) {
              implicit val cont = contact
              Transactions.save(TransactionsXLS.apply4(myTrans))
              true
            } else
              false
        }
      }
    val groupedStats = stats.groupBy(identity).mapValues(_.size)
    counter(
      if (groupedStats contains true)
        groupedStats(true)
      else
        0,
      if (groupedStats contains false)
        groupedStats(false)
      else
        0)
  }

}